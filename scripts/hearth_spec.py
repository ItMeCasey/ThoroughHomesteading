"""Hearth tier shapes in world coordinates (0..32, two blocks tall); make_hearth_models.py splits them at y=16."""
"""Hearth tier specs in world coordinates (0..32, two blocks tall). Each cube: (name, material, from, to[, rot]) ; rot = (origin, axis, angle)."""
def box(n,m,a,b,rot=None,lit=False): return dict(name=n,mat=m,f=a,t=b,rot=rot,lit=lit)

def base_fire(ringmat, ring, extra=()):
    out=[]
    out+=ring
    out.append(box('ash','ash',[4,1,4],[12,2,12]))
    out+=[box('log_a','logs',[4,2,5],[12,4,7]),box('log_b','logs',[4,2,9],[12,4,11]),
          box('log_c','logs',[5,4,4],[7,6,12]),box('log_d','logs',[9,4,4],[11,6,12])]
    out+=[box('flame_core','flame',[6.5,6,6.5],[9.5,11,9.5],lit=True),box('flame_a','flame',[5,6,5.5],[7,9,7.5],lit=True),
          box('flame_b','flame',[9,6,8.5],[11,9,10.5],lit=True),box('flame_c','flame',[5.5,6,9],[7.5,8,11],lit=True),
          box('flame_d','flame',[8.5,6,5],[10.5,8,7],lit=True)]
    return out

def stone():
    s='stone'
    ring=[box('plinth',s,[1,0,1],[15,1,15]),box('ring_n',s,[3,1,2],[14,4,4]),box('ring_s',s,[2,1,12],[14,4,14]),
          box('ring_w',s,[2,1,3],[4,4,12]),box('ring_e',s,[12,1,4],[14,4,12])]
    e=base_fire(s,ring)
    e+=[box('post','pole',[1,1,1],[3,31,3]),box('arm','pole',[3,28,1],[10,30,3]),box('clapper','pole',[8.5,21,1.5],[9.5,22,2.5]),
        box('bell_neck','bell',[8.25,26,1.25],[9.75,28,2.75]),box('bell_body','bell',[7.5,23,0.5],[10.5,26,3.5]),box('bell_lip','bell',[7,22,0],[11,23,4])]
    return e

def iron():
    s='iron'; d='dark'
    ring=[box('plinth',s,[0,0,0],[16,1,16]),box('ring_n',s,[3,1,2],[14,5,4]),box('ring_s',s,[2,1,12],[14,5,14]),
          box('ring_w',s,[2,1,3],[4,5,12]),box('ring_e',s,[12,1,4],[14,5,12]),
          box('rim_n',d,[3,5,2],[14,6,4]),box('rim_s',d,[2,5,12],[14,6,14]),box('rim_w',d,[2,5,3],[4,6,12]),box('rim_e',d,[12,5,4],[14,6,12]),
          box('stud_se',d,[12,5,12],[14,7,14]),box('stud_ne',d,[12,5,2],[14,7,4]),box('stud_sw',d,[2,5,12],[4,7,14])]
    e=base_fire(s,ring)
    e+=[box('post',s,[1,1,1],[3,30,3]),box('collar',d,[0.5,1,0.5],[3.5,3,3.5]),box('band_a',d,[0.75,12,0.75],[3.25,13,3.25]),
        box('band_b',d,[0.75,21,0.75],[3.25,22,3.25]),box('cap',d,[0.5,30,0.5],[3.5,31,3.5]),box('finial',s,[1.25,31,1.25],[2.75,32,2.75]),
        box('arm',d,[3,28,1],[11,30,3]),box('arm_tip',s,[11,27.5,1],[12,30.5,3]),
        box('brace',d,[3,24,1.5],[8.66,25,2.5],rot=([3,24.5,2],'z',45)),
        box('clapper',d,[8.5,20.5,1.5],[9.5,22,2.5]),
        box('bell_neck','bell',[8.25,26,1.25],[9.75,28,2.75]),box('bell_body','bell',[7,23,0],[11,26,4]),box('bell_lip','bell',[6.5,22,0],[11.5,23,4])]
    return e

def golden():
    g='gold'; b='bronze'; r='gem'
    ring=[box('plinth',b,[0,0,0],[16,1,16]),box('plinth2',g,[1,1,1],[15,2,15]),
          box('ring_n',g,[3.5,2,2],[12.5,5,4]),box('ring_s',g,[3.5,2,12],[12.5,5,14]),
          box('ring_w',g,[2,2,3.5],[4,5,12.5]),box('ring_e',g,[12,2,3.5],[14,5,12.5]),
          box('rim_n',b,[3.5,5,2],[12.5,6,4]),box('rim_s',b,[3.5,5,12],[12.5,6,14]),box('rim_w',b,[2,5,3.5],[4,6,12.5]),box('rim_e',b,[12,5,3.5],[14,6,12.5]),
          box('gem_s',r,[7,3,13.5],[9,5,14.5]),box('gem_n',r,[7,3,1.5],[9,5,2.5]),box('gem_w',r,[1.5,3,7],[2.5,5,9]),box('gem_e',r,[13.5,3,7],[14.5,5,9])]
    e=base_fire(g,ring)
    for i,(x,z) in enumerate([(1.5,1.5),(12.5,1.5),(1.5,12.5),(12.5,12.5)]):
        e+=[box(f'pillar_{i}',g,[x,2,z],[x+2,27,z+2]),box(f'pbase_{i}',b,[x-0.5,2,z-0.5],[x+2.5,4,z+2.5]),
            box(f'pgem_{i}',r,[x-0.25,13,z-0.25],[x+2.25,15,z+2.25]),box(f'pcap_{i}',b,[x-0.5,25,z-0.5],[x+2.5,27,z+2.5])]
    e+=[box('beam_n',g,[3.5,25,1.5],[12.5,27,3.5]),box('beam_s',g,[3.5,25,12.5],[12.5,27,14.5]),
        box('beam_w',g,[1.5,25,3.5],[3.5,27,12.5]),box('beam_e',g,[12.5,25,3.5],[14.5,27,12.5]),
        box('roof_1',b,[0,27,0],[16,28,16]),box('roof_2',g,[1.5,28,1.5],[14.5,29.5,14.5]),box('roof_3',b,[3.5,29.5,3.5],[12.5,30.5,12.5]),
        box('roof_4',g,[5.5,30.5,5.5],[10.5,31.5,10.5]),box('roof_gem',r,[7,31.5,7],[9,32,9]),
        box('chain',b,[7.5,24,7.5],[8.5,27,8.5]),
        box('bell_neck','bell',[7,22.5,7],[9,24,9]),box('bell_body','bell',[6.5,19.5,6.5],[9.5+0.5,22.5,9.5+0.5]),box('bell_lip','bell',[5.5,18.5,5.5],[10.5,19.5,10.5]),
        box('clapper',b,[7.5,17,7.5],[8.5,18.5,8.5])]
    return e

TIERS={'stone':stone,'iron':iron,'golden':golden}
