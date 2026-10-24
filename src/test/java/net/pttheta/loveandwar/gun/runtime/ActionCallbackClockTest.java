package net.pttheta.loveandwar.gun.runtime;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionCallbackClockTest {
    private record Event(int action, long trigger, int progress) {}
    private static ActionCallbackClock clock(float... delays) { return new ActionCallbackClock(delays.length, i -> delays[i]); }
    private static void trigger(ActionCallbackClock clock, List<Event> events) {
        clock.trigger(i -> true, (i, trigger, progress) -> events.add(new Event(i, trigger, progress)));
    }
    private static void tick(ActionCallbackClock clock, List<Event> events) {
        clock.tick((i, trigger, progress) -> events.add(new Event(i, trigger, progress)));
    }

    @Test void immediateCallbacksSeePreIncrementCountAndShareInvocationIndex() {
        var clock = clock(0, 0); var events = new ArrayList<Event>(); var observed = new ArrayList<Long>();
        for (int n = 0; n < 2; n++) clock.trigger(i -> true, (i, trigger, progress) -> {
            events.add(new Event(i, trigger, progress)); observed.add(clock.triggers());
        });
        assertEquals(List.of(new Event(0,0,0), new Event(1,0,0), new Event(0,1,0), new Event(1,1,0)), events);
        assertEquals(List.of(0L,0L,1L,1L), observed);
        assertEquals(2, clock.triggers());
    }

    @Test void nonzeroFractionalAndNegativeDelaysQueueBeforeTruncation() {
        var clock = clock(.9F, -.9F, 2.9F, 0); var events = new ArrayList<Event>();
        trigger(clock,events); tick(clock,events); tick(clock,events);
        assertEquals(List.of(new Event(3,0,0),new Event(0,1,1),new Event(1,1,1),new Event(2,1,2)),events);
        assertFalse(clock.hasPending());
    }

    @Test void retriggerOverwritesPendingSlotAndDueUsesCurrentCount() {
        var clock = clock(2); var events = new ArrayList<Event>();
        trigger(clock,events); tick(clock,events); trigger(clock,events); tick(clock,events);
        assertTrue(events.isEmpty());
        tick(clock,events);
        assertEquals(List.of(new Event(0,2,3)),events);
    }

    @Test void filteredTriggerKeepsExcludedPendingActionAndStillAdvancesCount() {
        var clock = clock(2,0); var events = new ArrayList<Event>();
        trigger(clock,events); tick(clock,events);
        clock.trigger(i -> i==1,(i,trigger,progress)->events.add(new Event(i,trigger,progress)));
        tick(clock,events);
        assertEquals(List.of(new Event(1,0,0),new Event(1,1,1),new Event(0,2,2)),events);
    }

    @Test void recursiveImmediateInvocationDoesNotChangeOuterCallbackArgument() {
        var clock = clock(0,0); var events = new ArrayList<Event>(); var observed = new ArrayList<Long>();
        clock.trigger(i -> true,(i,index,progress)->{
            events.add(new Event(i,index,progress)); observed.add(clock.triggers());
            if(i==0) clock.trigger(j -> true,(j,n,p)->{
                events.add(new Event(j,n,p)); observed.add(clock.triggers());
            });
        });
        assertEquals(List.of(new Event(0,0,0),new Event(0,0,0),new Event(1,0,0),new Event(1,0,0)),events);
        assertEquals(List.of(0L,0L,0L,1L),observed);
        assertEquals(2,clock.triggers());
    }

    @Test void dueCallbackRequeueIsRemovedWithCompletedSourceKey() {
        var clock = clock(1); var events = new ArrayList<Event>();
        trigger(clock,events);
        clock.tick((i,index,progress)->{
            events.add(new Event(i,index,progress)); trigger(clock,events);
        });
        assertFalse(clock.hasPending(),"Source removes completed keys after all callbacks, including synchronous requeues");
        tick(clock,events);
        assertEquals(List.of(new Event(0,1,1)),events);
        assertEquals(2,clock.triggers());
    }

    @Test void durationBoundaryDoesNotIncludeDelaysAndRetriggersExtendIt() {
        var clock = clock(100); var events = new ArrayList<Event>();
        trigger(clock,events); tick(clock,events); tick(clock,events);
        assertFalse(clock.pastDuration(2));
        tick(clock,events); assertTrue(clock.pastDuration(2)); assertTrue(clock.hasPending());
        trigger(clock,events); assertFalse(clock.pastDuration(2));
        tick(clock,events); assertFalse(clock.pastDuration(2));
        tick(clock,events); assertTrue(clock.pastDuration(2));
        assertTrue(events.isEmpty(),"Owning lifecycle can discard a still-pending action after the group duration");
    }

    @Test void equalDelaySlotsStayDistinctAndRetainAuthoredOrder() {
        var clock = clock(1,1); var events = new ArrayList<Event>();
        trigger(clock,events); tick(clock,events);
        assertEquals(List.of(new Event(0,1,1),new Event(1,1,1)),events);
    }

    @Test void localAndAuthoritativeClocksRemainIndependentUnderDifferentDeliveryTimes() {
        var local = clock(0,2); var server = clock(0,2);
        var localEvents = new ArrayList<Event>(); var serverEvents = new ArrayList<Event>();
        trigger(local,localEvents); tick(local,localEvents); tick(local,localEvents);
        assertEquals(0,server.triggers()); assertEquals(0,server.progress());
        trigger(server,serverEvents); tick(server,serverEvents); tick(server,serverEvents);
        assertEquals(localEvents,serverEvents);
        assertNotSame(local,server);
    }
}
