import importlib.util
import json
import os
from pathlib import Path
import secrets
import shlex
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('production', ROOT / 'scripts/production.py')


class ProductionPreparationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.module = importlib.util.module_from_spec(SPEC)
        SPEC.loader.exec_module(cls.module)

    def test_prepare_preserves_credentials_oauth_and_existing_proxy_settings(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            original = secrets.token_hex(32)
            (root / '.env').write_text('POSTGRES_DB=app\nPOSTGRES_USER=owner\nPOSTGRES_PASSWORD=' + original + '\nAPP_JWT_SECRET=' + original + '\n')
            auth = root / 'cli-proxy/auths/account.json'
            auth.parent.mkdir(parents=True)
            auth.write_text('oauth must survive')
            self.module.prepare(root)
            values = self.module.settings(root)
            self.assertEqual(values['APP_JWT_SECRET'], original)
            self.assertEqual(values['POSTGRES_PASSWORD'], original)
            self.assertEqual(len({values[k] for k in self.module.SECRETS}), len(self.module.SECRETS))
            config_path = root / 'private/cli-proxy/config.yaml'
            config = json.loads(config_path.read_text())
            self.assertEqual(config['access']['api-keys'], [values['AI_CLIPROXY_API_KEY']])
            self.assertEqual(config['oauth']['auth-dir'], '/auths')
            self.assertNotIn('api-keys', config)
            config['models'] = {'catalog': '/custom/catalog.json'}
            config_path.write_text(json.dumps(config))
            self.module.prepare(root)
            self.assertEqual(json.loads(config_path.read_text())['models'], config['models'])
            self.assertEqual(auth.read_text(), 'oauth must survive')
            self.assertEqual((root / '.env').stat().st_mode & 0o777, 0o600)
            self.assertEqual(config_path.stat().st_mode & 0o777, 0o600)

    def test_canonical_compose_env_parsing_and_no_shell_execution(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / '.env').write_text("POSTGRES_PASSWORD='literal$with#symbols'\nAPP_JWT_SECRET='$(touch SHOULD_NOT_EXIST)'\n")
            values = self.module.settings(root)
            self.assertEqual(values['POSTGRES_PASSWORD'], 'literal$with#symbols')
            self.assertEqual(values['APP_JWT_SECRET'], '$(touch SHOULD_NOT_EXIST)')
            self.assertFalse((root / 'SHOULD_NOT_EXIST').exists())

    def test_malformed_private_config_never_exposes_its_contents(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / '.env').write_text('POSTGRES_DB=app\nPOSTGRES_USER=owner\n')
            self.module.prepare(root)
            (root / 'frontend').mkdir()
            (root / 'frontend/nginx.prod.conf').write_text('fixture')
            config = root / 'private/cli-proxy/config.yaml'
            sensitive = secrets.token_hex(32)
            config.write_text('management: [\n  secret-key: ' + sensitive + '\n')
            with self.assertRaises(ValueError) as failure:
                self.module.check(root, self.module.settings(root))
            self.assertNotIn(sensitive, str(failure.exception))

    def test_private_management_key_cannot_be_missing_weak_or_reuse_client_key(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / '.env').write_text('POSTGRES_DB=app\nPOSTGRES_USER=owner\n')
            self.module.prepare(root)
            (root / 'frontend').mkdir()
            (root / 'frontend/nginx.prod.conf').write_text('fixture')
            config = root / 'private/cli-proxy/config.yaml'
            proxy = json.loads(config.read_text())
            values = self.module.settings(root)
            for key in ['', 'weak-management-key', values['AI_CLIPROXY_API_KEY']]:
                with self.subTest(key_type='missing' if not key else 'invalid'):
                    proxy['management']['secret-key'] = key
                    config.write_text(json.dumps(proxy))
                    with self.assertRaisesRegex(ValueError, 'management key'):
                        self.module.check(root, values)

    def test_network_overlap_only_permits_the_configured_edge_network(self):
        validate = self.module.validate_networks
        validate([{'Name': 'danasea_edge', 'IPAM': {'Config': [{'Subnet': '172.30.10.0/28'}]}}], [])
        for networks, routes in [
            ([{'Name': 'other', 'IPAM': {'Config': [{'Subnet': '172.30.0.0/16'}]}}], []),
            ([], [{'dst': '172.30.10.0/24', 'dev': 'vpn0'}]),
        ]:
            with self.assertRaises(ValueError):
                validate(networks, routes)

    def test_host_and_none_networks_without_ipam_subnets_are_ignored(self):
        self.module.validate_networks(
            [
                {'Name': 'host', 'IPAM': {'Config': None}},
                {'Name': 'none', 'IPAM': None},
            ],
            [],
        )

    def test_database_role_cannot_be_the_migration_owner(self):
        with self.assertRaises(ValueError):
            self.module.runtime_role_sql({'POSTGRES_APP_USER': 'owner', 'POSTGRES_USER': 'owner', 'POSTGRES_DB': 'app', 'POSTGRES_APP_PASSWORD': secrets.token_hex(32)})
        sql = self.module.runtime_role_sql({'POSTGRES_APP_USER': 'app', 'POSTGRES_USER': 'owner', 'POSTGRES_DB': 'app', 'POSTGRES_APP_PASSWORD': secrets.token_hex(32)})
        self.assertIn('NOSUPERUSER', sql)
        self.assertIn('flyway_schema_history', sql)
        self.assertIn('REVOKE INSERT, UPDATE, DELETE', sql)


class DeploymentGuardTest(unittest.TestCase):
    def test_private_proxy_addresses_match_compose_and_backend(self):
        with tempfile.TemporaryDirectory() as directory:
            envfile = Path(directory) / '.env'
            text = (ROOT / '.env.example').read_text()
            for key in ['POSTGRES_APP_PASSWORD', 'APP_CHECKIN_QR_SECRET', 'APP_PAYMENT_WEBHOOK_SECRET', 'SPRING_REDIS_PASSWORD', 'SPRING_RABBITMQ_PASSWORD', 'AI_CLIPROXY_API_KEY']:
                text = text.replace(key + '=\n', key + '=' + secrets.token_hex(32) + '\n')
            envfile.write_text(text)
            result = subprocess.run(['docker', 'compose', '--env-file', str(envfile), '-f', str(ROOT / 'docker-compose.prod.yml'), 'config', '--format', 'json'], capture_output=True, text=True, check=True)
            config = json.loads(result.stdout)
        network = config['networks']['app']['ipam']['config'][0]
        self.assertEqual(network, {'subnet': '172.30.10.0/28', 'gateway': '172.30.10.1'})
        frontend = config['services']['frontend']['networks']['app']['ipv4_address']
        nginx = (ROOT / 'frontend/nginx.prod.conf').read_text()
        self.assertIn('set_real_ip_from ' + network['gateway'] + ';', nginx)
        self.assertIn("internal-proxies: '" + frontend.replace('.', '\\.') + "'", (ROOT / 'backend/src/main/resources/application-prod.yml').read_text())
        for service in config['services'].values():
            for setting in ['mem_limit', 'memswap_limit', 'cpus', 'pids_limit']:
                self.assertNotIn(setting, service)
        self.assertNotIn('X-Forwarded-Proto https', (ROOT / 'frontend/nginx.conf').read_text())
        self.assertIn('PRODUCTION_DEPLOY_ENABLED', (ROOT / '.github/workflows/cd.yml').read_text())

    def test_preflight_failure_never_starts_or_pulls_containers(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            binary = root / 'bin'
            binary.mkdir()
            log = root / 'calls'
            for name in ['git', 'flock', 'python3', 'docker']:
                path = binary / name
                path.write_text('#!/bin/sh\nprintf "%s\\n" "' + name + ' $*" >> ' + shlex.quote(str(log)) + '\n' + ('exit 17\n' if name == 'python3' else 'exit 0\n'))
                path.chmod(0o700)
            environment = {**os.environ, 'APP_DIR': str(root), 'PATH': str(binary) + ':' + os.environ['PATH'], 'SSH_ORIGINAL_COMMAND': 'deploy ' + 'a' * 40}
            result = subprocess.run(['bash', str(ROOT / 'scripts/deploy.sh')], env=environment, capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            calls = log.read_text()
            self.assertIn('python3 scripts/production.py check', calls)
            self.assertNotIn('docker ', calls)

    def test_forced_command_rejects_extra_arguments_before_git(self):
        result = subprocess.run(['bash', str(ROOT / 'scripts/deploy.sh')], env={**os.environ, 'SSH_ORIGINAL_COMMAND': 'deploy ' + 'a' * 40 + ' extra'}, capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('Usage:', result.stderr)


if __name__ == '__main__':
    unittest.main()
