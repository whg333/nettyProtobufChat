import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {

	private static final Random random = new Random();
	
	private static final ConcurrentHashMap<Integer, String> map = 
			new ConcurrentHashMap<Integer, String>();
	
//	private static final HashMap<Integer, String> map = 
//			new HashMap<Integer, String>();
	
	private static final AtomicInteger index = new AtomicInteger();
	
	private static volatile boolean isOccuptException = false;
	
	public static void main(String[] args) {
		for(int i=0;i<500;i++){
			new PutThread("PutThread-"+i).start();
		}
		for(int i=0;i<400;i++){
			new RemoveThread("RemoveThread-"+i).start();
		}
		for(int i=0;i<350;i++){
			new ForEachThread("ForEachThread-"+i).start();
		}
	}
	
	private static class PutThread extends Thread{
		private PutThread(String name){
			super(name);
		}
		@Override
		public void run(){
			try {
				for(;;){
					int i = index.incrementAndGet();
					String s = String.valueOf(i);
					map.put(i, s);
					System.out.println(getName()+":put "+s);
					Thread.sleep(random.nextInt(1000));
					if(isOccuptException){
						break;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class RemoveThread extends Thread{
		private RemoveThread(String name){
			super(name);
		}
		@Override
		public void run(){
			try {
				for(;;){
					int i = random.nextInt(index.get());
					String s = map.remove(i);
					if(s == null){
						continue;
					}
					System.out.println(getName()+":"+s+" had been removed");
					Thread.sleep(random.nextInt(1000));
					if(isOccuptException){
						break;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class ForEachThread extends Thread{
		private ForEachThread(String name){
			super(name);
		}
		@Override
		public void run(){
			try {
				for(;;){
					Thread.sleep(random.nextInt(2000));
					for(String s:map.values()){
						System.out.println(getName()+":I am "+s);
					}
					if(isOccuptException){
						break;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch(Exception e){
				e.printStackTrace();
				System.out.println("occupt an Exception:"+e.getMessage());
				isOccuptException = true;
			}
		}
	}
	
}
