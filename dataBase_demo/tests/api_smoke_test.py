"""Real Backend tests for this package only. Login/GET; no workflow mutation."""
from pathlib import Path
from datetime import datetime, timezone
from decimal import Decimal
from concurrent.futures import ThreadPoolExecutor
import argparse
import base64
import getpass
import hashlib
import json
import re
import runpy
import sys
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
HTTP=runpy.run_path(str(ROOT/'installer/demo_api.py'))
TABLES=('users','vendors','categories','services','service_options','service_slots',
        'service_images','vendor_documents','service_safety_documents')


def run(base,password,container=None):
    records={t:json.loads((ROOT/'data'/f'{t}.json').read_text(encoding='utf8'))['records'] for t in TABLES}
    rows={t:[dict(r['sqlRow']) for r in rr] for t,rr in records.items()}
    storage_path=ROOT/'runtime/storage.json'
    storage=json.loads((storage_path if storage_path.exists() else ROOT/'data/storage_manifest.json').read_text(encoding='utf8'))
    for t,key in [('service_images','url'),('vendor_documents','file_url'),('service_safety_documents','file_url')]:
        for record,row in zip(records[t],rows[t]):
            if record['mediaSha256'] in storage:row[key]=storage[record['mediaSha256']]['url']
    steps=[];tokens={};returned_media={}
    counts=dict(vendorLogin=0,adminLogin=0,customerLogin=0,vendorProfiles=0,services=0,options=0,
                slots=0,images=0,vendorDocuments=0,serviceSafetyDocuments=0,storageLinks=0)
    def add(label,path,status,n,ok,**extra):
        steps.append(dict(step=label,endpoint=path,httpStatus=status,count=n,result='PASS' if ok else 'FAIL',**extra))
    def request(path,token=None,method='GET',data=None):
        assert method=='GET' or path=='/api/auth/login','Mutation forbidden'
        return HTTP['request'](base,path,method,token,data,container)
    def camel(k):
        return 'maxWaveM' if k=='max_wavem' else re.sub(r'_([a-z])',lambda m:m[1].upper(),k)
    def same(a,b,k):
        if a is None or b is None:return a is b
        if k in ('price','rating_avg','avg_rating','latitude','longitude','min_wind_kmh','max_wavem'):return Decimal(str(a))==Decimal(str(b))
        if k in ('start_time','end_time'):return str(a).rstrip('0').rstrip(':')==str(b).rstrip('0').rstrip(':')
        return a==b
    def mismatch(row,body,omit=()):
        if not isinstance(body,dict):return ['response-not-object']
        return [k for k,v in row.items() if k not in ('created_at','updated_at')+omit and
                (camel(k) not in body or not same(v,body[camel(k)],k))]
    def check_list(label,path,token,expected,omit=()):
        st,b=request(path,token);actual=HTTP['content'](b);by_id={r.get('id'):r for r in actual}
        diff={r['id']:mismatch(r,by_id.get(r['id']),omit) for r in expected};diff={k:v for k,v in diff.items() if v}
        ok=st==200 and len(actual)==len(expected) and set(by_id)=={r['id'] for r in expected} and not diff
        add(label,'GET '+path,st,len(actual),ok,fieldMismatches=diff)
        return ok,actual
    for u in rows['users']:
        st,b=request('/api/auth/login',method='POST',data={'email':u['email'],'password':password})
        ok=st==200 and isinstance(b,dict) and b.get('userId')==u['id'] and b.get('role')==u['role'] and bool(b.get('accessToken'))
        if ok:
            token=b['accessToken']
            claims=json.loads(base64.urlsafe_b64decode(token.split('.')[1]+'==='))
            ok=claims.get('sub')==u['email'] and claims.get('role')==u['role'] and claims.get('exp',0)>datetime.now(timezone.utc).timestamp()
            if ok:tokens[u['id']]=token
        add('LOGIN_'+u['role'],'POST /api/auth/login',st,int(ok),ok,fixtureEmail=u['email'])
        if ok:
            counts[{'VENDOR':'vendorLogin','ADMIN':'adminLogin','CUSTOMER':'customerLogin'}[u['role']]]+=1
            status,profile=request('/api/users/me',token)
            fields={k:u[k] for k in ('id','email','full_name','avatar_url','locale','role','is_email_verified')}
            add('PROFILE','GET /api/users/me',status,1,status==200 and not mismatch(fields,profile),fixtureUserId=u['id'])
    for v in rows['vendors']:
        token=tokens.get(v['user_id'])
        if not token:continue
        st,p=request('/api/vendor/profile',token)
        ok=st==200 and not mismatch(v,p,('verified_by','verified_at'))
        add('VENDOR_PROFILE','GET /api/vendor/profile',st,1,ok,vendorId=v['id']);counts['vendorProfiles']+=int(ok)
        services=[s for s in rows['services'] if s['vendor_id']==v['id']]
        ok,_=check_list('VENDOR_SERVICES','/api/vendor/services',token,services,('search_vector',))
        for s in services:
            path='/api/vendor/services/'+s['id'];st,detail=request(path,token)
            images=[i for i in rows['service_images'] if i['service_id']==s['id']]
            actual=detail.get('images',[]) if isinstance(detail,dict) else []
            ids={i.get('id'):i for i in actual}
            image_ok=set(ids)=={i['id'] for i in images} and len(actual)==len(images) and all(not mismatch(i,ids.get(i['id'])) for i in images)
            detail_ok=st==200 and not mismatch(s,detail,('search_vector',)) and image_ok
            add('SERVICE_DETAIL','GET '+path,st,1,detail_ok,images=len(actual),serviceId=s['id'])
            counts['services']+=int(detail_ok)
            if image_ok:
                counts['images']+=len(actual)
                for i in actual:returned_media[i['id']]=i['url']
            for table,suffix,key in [('service_options','options','options'),('service_slots','slots','slots')]:
                expected=[r for r in rows[table] if r['service_id']==s['id']]
                ok,_=check_list('VENDOR_'+suffix.upper(),path+'/'+suffix,token,expected)
                counts[key]+=len(expected) if ok else 0
        docs=[d for d in rows['vendor_documents'] if d['vendor_id']==v['id']]
        ok,actual=check_list('VENDOR_DOCUMENTS','/api/vendor/documents',token,docs)
        if ok:
            counts['vendorDocuments']+=len(actual)
            for d in actual:returned_media[d['id']]=d['fileUrl']
        foreign=next(s for s in rows['services'] if s['vendor_id']!=v['id'])
        path='/api/vendor/services/'+foreign['id'];st,_=request(path,token)
        add('OTHER_VENDOR_DENIED','GET '+path,st,0,st==403)
    admin_user=next(u for u in rows['users'] if u['role']=='ADMIN');admin=tokens.get(admin_user['id'])
    if admin:
        check_list('ADMIN_VENDORS','/api/admin/vendors?page=0&size=100',admin,rows['vendors'])
        check_list('ADMIN_SERVICES','/api/admin/services',admin,rows['services'],('search_vector',))
        for s in rows['services']:
            docs=[d for d in rows['service_safety_documents'] if d['service_id']==s['id']]
            path='/api/admin/services/'+s['id']+'/safety-documents'
            ok,actual=check_list('SERVICE_SAFETY_DOCUMENTS',path,admin,docs)
            if ok:
                counts['serviceSafetyDocuments']+=len(actual)
                for d in actual:returned_media[d['id']]=d['fileUrl']
    by_url={entry['url']:digest for digest,entry in storage.items()}
    def download(url):
        if url not in by_url:return url,0,False
        try:
            with urllib.request.urlopen(url,timeout=45) as response:raw,status=response.read(),response.status
            return url,status,status==200 and hashlib.sha256(raw).hexdigest()==by_url[url]
        except Exception:return url,0,False
    with ThreadPoolExecutor(max_workers=6) as pool:
        verified={url:(status,ok) for url,status,ok in pool.map(download,set(returned_media.values()))}
    for rid,url in returned_media.items():
        st,ok=verified[url];add('STORAGE_READBACK','GET storage URL returned by Backend',st,1,ok,recordId=rid)
        counts['storageLinks']+=int(ok)
    expected=dict(vendorLogin=9,adminLogin=1,customerLogin=2,vendorProfiles=9,services=24,options=33,
                  slots=24,images=72,vendorDocuments=18,serviceSafetyDocuments=len(rows['service_safety_documents']),storageLinks=114)
    add('TOTAL_COVERAGE','Backend API + real storage',None,sum(counts.values()),counts==expected,expected=expected,actual=counts)
    return dict(executedAt=datetime.now(timezone.utc).isoformat(),baseUrl=base,counts=counts,
                result='PASS' if all(s['result']=='PASS' for s in steps) else 'FAIL',
                steps=steps,workflowMutations=False,documentsLegallyVerified=False)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url',default='http://127.0.0.1:8080')
    parser.add_argument('--api-container')
    parser.add_argument('--password-stdin',action='store_true')
    parser.add_argument('--output',type=Path,default=ROOT/'runtime/api_result.json')
    a=parser.parse_args();password=sys.stdin.readline().rstrip('\r\n') if a.password_stdin else getpass.getpass('DEMO password (hidden): ')
    try:
        result=run(a.base_url,password,a.api_container)
        a.output.parent.mkdir(exist_ok=True)
        a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        print(json.dumps({'result':result['result'],'counts':result['counts'],'output':str(a.output)}))
        sys.exit(0 if result['result']=='PASS' else 1)
    except Exception as exc:
        print(json.dumps({'result':'RUN_ERROR','exceptionType':type(exc).__name__}));sys.exit(2)
