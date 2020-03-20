package jvm;

import java.util.HashSet;

/**
 * @author xyf
 * @date 20200319
 * @VMArgs： -XX:PermSize=6M -XX:MaxPermSize=6M
 * 设置非堆模式内存为6M
 * */
public class RuntimeConstantPoolOOM {
	public static void main(String[] args) {
		// 使用set保持着常量池的引用,,避免Full GC的回收行为
		HashSet<String> set = new HashSet<>();
		// short范围内足以让6MB的PermSize产生OOM
		short i=0;
		while (true) {
			set.add(String.valueOf(i++).intern());
		}
	}
}
