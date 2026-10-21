"""Real API smoke/workflow checks. Never stores tokens, passwords or fake payments."""
from pathlib import Path
import argparse
import getpass
import json
import subprocess
import sys
import urllib.request
import urllib.error

ROOT=Path(__file__).resolve().parents[1]


def request(base,path,method='GET',token=None,data=None,container=None):
    headers={'Accept':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    body=None
    if data is not None:
        headers['Content-Type']='application/json'
        body=json.dumps(data,ensure_ascii=False)
    if container:
        cfg='url = '+json.dumps(base+path)+'\nrequest = '+json.dumps(method)+'\n'
        for k,v in headers.items():cfg+='header = '+json.dumps(k+': '+v)+'\n'
        if body is not None:cfg+='data = '+json.dumps(body,ensure_ascii=False)+'\n'
        p=subprocess.run(['docker','exec','-i',container,'curl','--silent','--show-error','--max-time','30',
            '--config','-','--write-out','\nDANASEA_HTTP_STATUS:%{http_code}'],input=cfg.encode('utf8'),capture_output=True,timeout=40)
        if p.returncode:return 0,None
        raw,_,code=p.stdout.rpartition(b'\nDANASEA_HTTP_STATUS:')
        status=int(code) if code.isdigit() else 0
    else:
        req=urllib.request.Request(base+path,method=method,headers=headers,data=None if body is None else body.encode('utf8'))
        try:
            with urllib.request.urlopen(req,timeout=30) as response:status,raw=response.status,response.read()
        except urllib.error.HTTPError as e:status,raw=e.code,e.read()
        except OSError:return 0,None
    try:return status,json.loads(raw)
    except ValueError:return status,None


def content(value):
    if isinstance(value,list):return value
    if isinstance(value,dict):
        for key in ('content','data','items','services'):
            if key in value:return content(value[key])
    return []
