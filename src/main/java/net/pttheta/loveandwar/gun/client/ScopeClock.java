package net.pttheta.loveandwar.gun.client;

/** Original Scope ActionInstance magnitudes and per-action delayed-client-trigger slots. */
final class ScopeClock {
    private final float[] delays, magnitudes;
    private final int[] remaining;
    private final boolean[] pending;
    private long trigger;

    ScopeClock(float[] delays, long trigger) {
        this.delays=delays.clone();magnitudes=new float[delays.length];remaining=new int[delays.length];pending=new boolean[delays.length];
        this.trigger=trigger;queueDelayed();
    }
    void observe(long trigger) {
        if(this.trigger==trigger)return;
        this.trigger=trigger;queueDelayed();
    }
    private void queueDelayed() {
        for(int i=0;i<delays.length;i++)if(delays[i]!=0){pending[i]=true;remaining[i]=(int)delays[i];}
    }
    void tick() {
        for(int i=0;i<magnitudes.length;i++) {
            if(pending[i]) {
                // Original skips OnTickClient on the due tick too; its empty trigger runs after that gate.
                if(--remaining[i]<=0)pending[i]=false;
            }else magnitudes[i]+=(1-magnitudes[i])*.05F;
        }
    }
    boolean hidden(){for(float magnitude:magnitudes)if(magnitude>=.1F)return true;return false;}
}
