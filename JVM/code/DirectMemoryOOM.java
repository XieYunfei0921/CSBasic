package jvm;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author xyf
 * @date 20200319
 * @VMArgs： -Xmx20M -XX:MaxDirectMemorySize=10M
 * */
public class DirectMemoryOOM {
	public static final int _1MB=1024*1024;

	public static void main(String[] args) throws Exception{
		Field field = Unsafe.class.getDeclaredFields()[0];
		field.setAccessible(true);
		Unsafe f= (Unsafe) field.get(null);
		while (true) {
			// 这个操作实际向操作系统申请了内存空间,不停的向操作系统申请内存空间(1MB)
			f.allocateMemory(_1MB);
		}
	}
}
