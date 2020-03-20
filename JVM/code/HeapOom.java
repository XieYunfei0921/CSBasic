package jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xyf
 * @date 20200319
 * @content java堆内存OOM测试
 * VM Args： -Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError
 * */
public class HeapOom {
	static class ObjectOom{

	}
	public static void main(String[] args) {
		List<ObjectOom> list=new ArrayList<ObjectOom>();
		while (true){
			list.add(new ObjectOom());
		}
	}
}
