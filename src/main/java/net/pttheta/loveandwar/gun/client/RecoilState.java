package net.pttheta.loveandwar.gun.client;

/** Pinned RecoilManager's global pending/stack recurrence, independent of item-model animation. */
public final class RecoilState {
    private float pendingYaw,pendingPitch,yaw,pitch,lastYaw,lastPitch;
    public void add(float yaw,float pitch){pendingYaw+=yaw;pendingPitch+=pitch;}
    public void shot(float horizontal,float vertical,long triggerIndex,double gaussian){
        // ShootAction starts each maximum at zero and adds once after visiting all shot results.
        add(Math.max(0,horizontal)*(float)gaussian,Math.max(0,vertical)*pitchFactor(triggerIndex));
    }
    public static float pitchFactor(long triggerIndex){return .125F+.375F*(float)Math.exp(-triggerIndex*.125F);}
    public void tick(){
        lastYaw=yaw;lastPitch=pitch;
        float dy=pendingYaw*.5F,dp=pendingPitch*.5F;
        yaw+=dy;pitch+=dp;yaw*=.75F;pitch*=.75F;pendingYaw-=dy;pendingPitch-=dp;
    }
    public float yaw(float partialTick){return lerp(lastYaw,yaw,partialTick);}
    public float pitch(float partialTick){return lerp(lastPitch,pitch,partialTick);}
    private static float lerp(float a,float b,float t){return a+(b-a)*Math.max(0,Math.min(1,t));}
}
