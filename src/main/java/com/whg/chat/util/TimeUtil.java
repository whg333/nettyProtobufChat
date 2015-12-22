package com.whg.chat.util;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * 
 * 时间工具类
 * @author luzj
 *
 */
public final class TimeUtil {
	
	/**真实的TimeUnit转换秒到天时的时间差*/
	private final static long actual_time_disfference = TimeUnit.HOURS.toMillis(8);
	
	/**hoolai 默认使用北京时间03点做为一天的分隔点, 该环境下的时间差*/
	private final static long default_time_disfference = TimeUnit.HOURS.toMillis(5);
	
    private static TimeSource source = new DefaultStandardTimeSource();
    
    private TimeUtil() {
        //最好还是别用实例了吧
    }
    
    public static void setSource(TimeSource timeSource){
        source = timeSource;
    }
    
    public static void useHackedStandardSource(){
    	if(!(source instanceof HackedStandardTimeSource)){
    		source = new HackedStandardTimeSource();
    	}
    }
    
    /**
     * 使用2010年为基准时间的TimeSource
     * 并且使用线程自增来改变时间, 不实时调用System.currentTimeMills;
     */
    public static void useHackedStampSource(){
        if(!(source instanceof HackedStampTimeSource)){
            source = new HackedStampTimeSource();
        }
    }
    
    /**
     * 使用2010年为基准时间的timeSource
     */
    public static void useStampTimeSource(){
        if(!(source instanceof DefaultStampTimeSource)) {
            source = new DefaultStampTimeSource();
        }
    }
    
    /**
     * 用于测试更改当前时间
     * @param currentTimeMillis
     */
    public static void changeCurrentTimeForTest(long currentTimeMillis) {
        source = new HackedTimeSource(currentTimeMillis);
    }
    
    
    public static long currentTimeMillis(){
        return source.currentTimeMillis();
    }
    
    public static long currentTimeSecond(){
        return source.currentTimeSecond();
    }
    
    public static long currentTimeDay(){
        return source.currentTimeDay();
    }
    
    public static long currentTimeHour(){
        return source.currentTimeHour();
    }
    
    public static long currentTimeMinute(){
    	return source.currentTimeMinute();
    }
    
    /**北京时间:零晨3点刷新一天制的天数(default)*/
    public static int currentDayInBeijing() {
    	return timeMillisToDaysInBeijing(source.currentTimeMillis());
    }
    
    /**北京时间:零晨0点刷新一天制的天数*/
    public static int currentDayInBeijingActual() {
    	return timeMillisToDaysInBeijingActual(source.currentTimeMillis());
    }
    
    /**北京时间:零晨3点刷新一天制的天数(default)*/
    public static int timeMillisToDaysInBeijing(long timeMillis) {
    	return (int) TimeUnit.MILLISECONDS.toDays(timeMillis + default_time_disfference);
    }
    
    /**北京时间:零晨0点刷新一天制的天数(default)*/
    public static int timeMillisToDaysInBeijingActual(long timeMillis) {
    	return (int) TimeUnit.MILLISECONDS.toDays(timeMillis + actual_time_disfference);
    }
    
    public static Date millsToDate(long mills){
        return source.millsToDate(mills);
    }
    
    public static Date secondToDate(long seconds){
        return source.secondToDate(seconds);
    }
    
    /**
     * time source
     * @author luzj
     */
    public static abstract class TimeSource {
    	
        private final long timeStamp;
        
        public abstract long currentTimeMillis();
        
        public TimeSource(long timeStamp) {
            this.timeStamp = timeStamp;
        }
        
        public TimeSource() {
            this(0);
        }
        
        public long currentTimeSecond() {
            return TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis());
        }
        
        public int currentTimeDay() {
            return (int) TimeUnit.MILLISECONDS.toDays(currentTimeMillis());
        }
        
        public int currentTimeHour() {
            return (int) TimeUnit.MILLISECONDS.toHours(currentTimeMillis());
        }
        
        public int currentTimeMinute() {
            return (int) TimeUnit.MILLISECONDS.toMinutes(currentTimeMillis());
        }
        
        public Date millsToDate(long mills) {
            return new Date(mills + timeStamp);
        }

        public Date secondToDate(long seconds) {
            return new Date(TimeUnit.SECONDS.toMillis(seconds) + timeStamp);
        }
        
    }
    
    public static void main(String[] args) {
		System.out.println(currentTimeHour());
		System.out.println(currentTimeMinute());
	}
    
    private static final long TIMESTAMP;
    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        TIMESTAMP = calendar.getTimeInMillis();
    }
    
    /**
     * 使用相跟2010年1月1号0时0分0秒时相对的时间间隔
     * @author luzj
     */
    public static final class DefaultStampTimeSource extends TimeSource {

        public DefaultStampTimeSource() {
            super(TIMESTAMP);
        }
        
        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis() - TIMESTAMP;
        }

    }
    
    public static final class DefaultStandardTimeSource extends TimeSource {

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

    }
    
    /**
     * 使用独立线程模拟时间时间变更
     * 可以更改当前时间, 默认使用系统时间
     * 用于时间模拟
     * @author spacetrek
     */
    public static final class HackedTimeSource extends TimeSource {

        private volatile long currentTimeMillis;
        
        public HackedTimeSource() {
            this(System.currentTimeMillis());
        }
        
        public HackedTimeSource(long currentTimeMillis) {
            this.currentTimeMillis = currentTimeMillis;
            init();
        }
        
        private void init(){
            new Thread(){
                @Override
                public void run() {
                    while(!Thread.interrupted()){
                        currentTimeMillis += 1000;
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            //ignore
                        }
                    }
                }
            }.start();
        }
        
        @Override
        public long currentTimeMillis() {
            return currentTimeMillis;
        }
        
    }
    
    /**
     * 使用独立线程自增加时间
     * 使用相跟2010年1月1号0时0分0秒时相对的时间间隔
     * @author luzj
     */
    private static final class HackedStampTimeSource extends TimeSource {

        public HackedStampTimeSource() {
            super(TIMESTAMP);
        }
        
        private volatile long currentTimeMillis;
        
        {
            currentTimeMillis = System.currentTimeMillis() - TIMESTAMP;
            new Thread(){
                @Override
                public void run() {
                    while(!Thread.interrupted()){
                        currentTimeMillis += 1000;
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            //ignore
                        }
                    }
                }
            }.start();
        }
        
        @Override
        public long currentTimeMillis() {
            return currentTimeMillis;
        }

    }
    
    
    /**
     * 使用独立线程自增加时间
     * @author luzj
     */
    public static final class HackedStandardTimeSource extends TimeSource {
        
        public HackedStandardTimeSource() {
        	
        }
        
        private volatile long currentTimeMillis;
        
        {
            currentTimeMillis = System.currentTimeMillis();
            new Thread(){
                @Override
                public void run() {
                    while(!Thread.interrupted()){
                        currentTimeMillis += 1000;
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            //ignore
                        }
                    }
                }
            }.start();
        }
        
        @Override
        public long currentTimeMillis() {
            return currentTimeMillis;
        }
        
    }
    
}
