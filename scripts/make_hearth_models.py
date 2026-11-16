"""Builds the two-block-tall Hearth models from hearth_spec.py: per tier a lower and a lit lower half, an upper half, the item model
and the blockstate (half=lower|upper, lit=true|false). Run make_hearth_textures.py first: python scripts/make_hearth_models.py
"""
import json, os, shutil, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from hearth_spec import TIERS
ROOT=os.path.abspath(os.path.join(os.path.dirname(__file__),'..'))
RES=os.path.join(ROOT,'common','src','main','resources','assets','thoroughhomesteading').replace(chr(92),'/')
MODID='thoroughhomesteading'
TEXFILE={
 'stone':{'stone':'stone_hearth_stone','pole':'stone_hearth_pole','bell':'stone_hearth_bell','ash':'hearth_ash','logs':'hearth_logs','flame':'hearth_flame'},
 'golden':{'gold':'golden_hearth_gold','bronze':'golden_hearth_bronze','gem':'golden_hearth_gem','bell':'golden_hearth_gold','ash':'hearth_ash','logs':'hearth_logs','flame':'hearth_flame'},
 'iron':{'iron':'iron_hearth_iron','dark':'iron_hearth_dark','bell':'iron_hearth_bell','ash':'hearth_ash','logs':'hearth_logs','flame':'hearth_flame'},
}
def faces(f,t,tex):
    w,h,d=[round(t[i]-f[i],4) for i in range(3)]
    n=lambda v:int(v) if float(v).is_integer() else v
    uv={'north':[0,0,n(w),n(h)],'east':[0,0,n(d),n(h)],'south':[0,0,n(w),n(h)],'west':[0,0,n(d),n(h)],'up':[0,0,n(w),n(d)],'down':[0,0,n(w),n(d)]}
    return {k:{'uv':v,'texture':'#'+tex} for k,v in uv.items()}
def clean(v): return int(v) if float(v).is_integer() else round(v,4)
def make(tier, cubes, dy, particle):
    mats=[]
    for c in cubes:
        if c['mat'] not in mats: mats.append(c['mat'])
    ids={m:str(i) for i,m in enumerate(mats)}
    textures={'particle':f'{MODID}:block/{particle}'}
    for m in mats: textures[ids[m]]=f"{MODID}:block/{TEXFILE[tier][m]}"
    els=[]
    for c in cubes:
        f=[clean(c['f'][0]),clean(c['f'][1]-dy),clean(c['f'][2])]; t=[clean(c['t'][0]),clean(c['t'][1]-dy),clean(c['t'][2])]
        e={'name':c['name'],'from':f,'to':t}
        if c['rot']:
            o,ax,ang=c['rot']; e['rotation']={'angle':ang,'axis':ax,'origin':[clean(o[0]),clean(o[1]-dy),clean(o[2])]}
        e['faces']=faces(f,t,ids[c['mat']])
        els.append(e)
    return {'credit':'Made with Blockbench','textures':textures,'elements':els}
def split(cubes):
    lower,upper=[],[]
    for c in cubes:
        y0,y1=c['f'][1],c['t'][1]
        if c['rot']:
            (lower if y1<=16 else upper).append(c); continue
        if y1<=16: lower.append(c)
        elif y0>=16: upper.append(c)
        else:
            a=dict(c); a['t']=[c['t'][0],16,c['t'][2]]; lower.append(a)
            b=dict(c); b['f']=[c['f'][0],16,c['f'][2]]; upper.append(b)
    return lower,upper
def w(path,obj):
    os.makedirs(os.path.dirname(path),exist_ok=True)
    open(path,'w',encoding='utf8',newline='\n').write(json.dumps(obj,indent=2)+'\n')
for tier,fn in TIERS.items():
    cubes=fn()
    unlit=[c for c in cubes if not c['lit']]
    lower,upper=split(unlit)
    lower_lit,_=split(cubes)
    p=f'{tier}_hearth'
    particle=TEXFILE[tier][{'stone':'stone','iron':'iron','golden':'gold'}[tier]]
    w(f'{RES}/models/block/{p}_lower.json',make(tier,lower,0,particle))
    w(f'{RES}/models/block/{p}_lower_lit.json',make(tier,lower_lit,0,particle))
    w(f'{RES}/models/block/{p}_upper.json',make(tier,upper,16,particle))
    item=make(tier,unlit,0,particle)
    item['display']={'gui':{'rotation':[30,225,0],'translation':[0,-5.5,0],'scale':[0.36,0.36,0.36]},
        'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[0.25,0.25,0.25]},
        'fixed':{'rotation':[0,180,0],'translation':[0,-5,0],'scale':[0.36,0.36,0.36]},
        'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[0.25,0.25,0.25]},
        'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,0,0],'scale':[0.3,0.3,0.3]}}
    w(f'{RES}/models/item/{p}.json',item)
    w(f'{RES}/blockstates/{p}.json',{'variants':{
        'half=lower,lit=false':{'model':f'{MODID}:block/{p}_lower'},'half=lower,lit=true':{'model':f'{MODID}:block/{p}_lower_lit'},
        'half=upper,lit=false':{'model':f'{MODID}:block/{p}_upper'},'half=upper,lit=true':{'model':f'{MODID}:block/{p}_upper'}}})
    w(f'{RES}/items/{p}.json',{'model':{'type':'minecraft:model','model':f'{MODID}:item/{p}'}})
    print(tier,len(lower),len(lower_lit),len(upper))
for dead in os.listdir(f'{RES}/models/block'):
    if dead.endswith(('_hearth.json','_hearth_lit.json')): os.remove(f'{RES}/models/block/{dead}'); print('removed',dead)
