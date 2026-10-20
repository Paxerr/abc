"""Regenerate checksums after an intentional package edit; never during install."""
from pathlib import Path
import hashlib

ROOT=Path(__file__).resolve().parents[1]


def main():
    files=[p for p in ROOT.rglob('*') if p.is_file() and p.name!='MANIFEST.sha256'
           and 'runtime' not in p.relative_to(ROOT).parts and '__pycache__' not in p.parts
           and p.suffix not in ('.pyc','.pyo')]
    lines=[hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.relative_to(ROOT).as_posix() for p in sorted(files)]
    (ROOT/'MANIFEST.sha256').write_text('\n'.join(lines)+'\n',encoding='utf8')
    print('Manifest files:',len(lines))


if __name__=='__main__':main()
