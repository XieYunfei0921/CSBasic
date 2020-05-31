package jvm;
/**
 * @author xyf
 * @date 20200319
 * -Xss128k
 * */
public class JavaVMStackSOF {
	// 设置栈深度计数值
	private int stackLen=1;
	public void stackLeak(){
		stackLen++;
		stackLeak();
	}

	public static void main(String[] args) {
		JavaVMStackSOF jsof= new JavaVMStackSOF();
		try {
			jsof.stackLeak();
		} catch (Throwable e) {
			System.out.println("stack len= " + jsof.stackLen);
			throw e;
		}
	}
}
