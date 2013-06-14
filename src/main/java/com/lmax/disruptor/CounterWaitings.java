package com.lmax.disruptor;

import java.util.concurrent.locks.LockSupport;

/**
 * ENUM way of {@link WaitStrategy}, includes origin BusySpinWaitStrategy, YieldingWaitStrategy, SleepingWaitStrategy.
 *
 * @author qinxian
 *
 */
public enum CounterWaitings implements WaitStrategy
{
    YIELDING
    {
        @Override
        public int initialValue()
        {
            return 100;
        }

        @Override
        public int retry(int counter)
        {
            if (0 == counter)
                Thread.yield();
            else
                return --counter;
            return counter;
        }
    },
    /** Seems this way best for AMD HT */
    ModestYield
    {
        @Override
        public int retry(int counter)
        {
            if ((counter & 1) == 1)
                Thread.yield();
            return --counter;
        }
    },
    /** Seems very slow for AMD HT on JDK8 */
    ModestSleep
    {
        @Override
        public int retry(int counter)
        {
            if ((counter & 1) == 1)
                LockSupport.parkNanos(1L);
            return --counter;
        }
    },
    Sleeping
    {
        @Override
        public int retry(int counter)
        {
            if (counter < 1)
                LockSupport.parkNanos(1L);
            else if (counter-- > 100)
                return counter;
            else
                Thread.yield(); // counter > 0 && counter<=100
            return counter;
        }
    },
    BusySpin
    {
    // since busy and not got right value, so just let thread do empty rest,
    // maybe JIT kill it:)
    },
    // comma? for successor code
    ;

    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier)
                                                                                                            throws AlertException
    {
        long availableSequence;
        int counter = initialValue();
        while ((availableSequence = dependentSequence.get()) < sequence)
        {
            barrier.checkAlert();
            counter = retry(counter);
        }

        return availableSequence;
    }

    protected int initialValue()
    {
        return 200;
    }

    protected int retry(int counter)
    {
        return counter;
    }

    @Override
    public void signalAllWhenBlocking()
    {
        // do nothing because non-blocking
    }

}
