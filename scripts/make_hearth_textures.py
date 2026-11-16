"""Draws the original Hearth block textures (one 16x16 tile per material: stone, iron, gold, bronze, gems, bell, logs, ash, flame).

Run before make_hearth_models.py: python scripts/make_hearth_textures.py
"""
import random, base64, io, os, sys
from PIL import Image
ROOT=os.path.abspath(os.path.join(os.path.dirname(__file__),'..'))
OUT=os.path.join(ROOT,'common','src','main','resources','assets','thoroughhomesteading','textures','block'); os.makedirs(OUT,exist_ok=True)
def tile(name, colors, pattern=None, seed=1, extra=None):
    r=random.Random(seed); im=Image.new('RGBA',(16,16))
    for y in range(16):
        for x in range(16):
            c=r.choice(colors); im.putpixel((x,y),c+(255,))
    if pattern: pattern(im,r)
    im.save(os.path.join(OUT,name+'.png'))
def cobble(im,r,dark=(88,88,88),lite=(150,150,150)):
    for x in range(16):
        for y in (0,5,10,15):
            im.putpixel((x,y),dark+(255,))
    for y in range(16):
        for x in ((0,8) if (y//5)%2==0 else (4,12)):
            im.putpixel((x,y),dark+(255,))
    for y in (1,6,11):
        for x in range(16):
            if r.random()<.4: im.putpixel((x,y),lite+(255,))
def grain(im,r):
    for y in (2,7,12):
        for x in range(16): im.putpixel((x,y),(70,48,28,255) if r.random()<.8 else (110,80,48,255))
def panels(im,r,dark,lite):
    for i in range(16):
        for p in ((i,0),(i,15),(0,i),(15,i)): im.putpixel(p,dark+(255,))
        for p in ((i,1),(1,i)): 
            if 0<i<15: im.putpixel(p,lite+(255,))
    for p in ((8,0),(8,15)): pass
def goldfx(im,r):
    for i in range(16):
        im.putpixel((i,0),(255,247,160,255)); im.putpixel((0,i),(255,240,140,255))
        im.putpixel((i,15),(176,120,24,255)); im.putpixel((15,i),(176,120,24,255))
def bellfx(im,r):
    for i in range(16):
        im.putpixel((i,0),(255,245,150,255)); im.putpixel((i,15),(170,110,20,255))
tile('hearth_logs',[(104,78,48),(98,72,44),(112,86,54),(92,66,40)],grain,1)
tile('hearth_ash',[(52,48,46),(60,56,54),(44,40,38),(70,50,40)],None,2)
tile('hearth_flame',[(255,140,24),(255,170,40),(255,210,70),(255,120,16),(255,236,120)],None,3)
tile('stone_hearth_stone',[(126,126,126),(118,118,118),(134,134,134),(112,112,112)],cobble,4)
tile('stone_hearth_pole',[(78,58,38),(72,52,34),(84,64,42)],lambda im,r:None,5)
tile('stone_hearth_bell',[(250,206,58),(242,196,50),(255,218,76)],bellfx,6)
tile('iron_hearth_iron',[(214,214,216),(206,206,208),(222,222,224),(198,198,200)],lambda im,r:panels(im,r,(120,120,126),(240,240,242)),7)
tile('iron_hearth_dark',[(60,60,66),(54,54,60),(68,68,74)],lambda im,r:None,8)
tile('iron_hearth_bell',[(250,206,58),(242,196,50),(255,218,76)],bellfx,9)
tile('golden_hearth_gold',[(250,222,72),(244,212,60),(255,232,96)],goldfx,10)
tile('golden_hearth_bronze',[(188,134,30),(176,122,24),(198,146,40)],lambda im,r:None,11)
tile('golden_hearth_gem',[(206,30,44),(190,24,38),(232,64,74),(160,16,30)],lambda im,r:None,12)
print(sorted(os.listdir(OUT)))
