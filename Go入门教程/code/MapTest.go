package main

import "fmt"

/**
	Map测试函数
 */
func main() {
	// 创建一个key是string, value是int的映射
	dict1:=make(map[string]int)
	fmt.Println(len(dict1))

	// 手动赋值
	dict2:= map[string]int{"sandee":26,"xwb":25,"xn":26,"liubo":34}
	for e := range dict2 {
		fmt.Println(e," --> ",dict2[e])
	}
	// 映射赋值
	dict2["maxiaodong"]=24
	fmt.Println(dict2["maxiaodong"])

	// 从映射中判断值是否存在
	age,exists:=dict2["maxiaodong"]
	if exists {
		fmt.Println(age)
	}else {
		fmt.Println("没有这个人")
	}

	// 从映射中获取值, 并判断值是否存在
	age2:=dict2["zwssx"]
	if age2>0 {
		fmt.Println("年龄",age2)
	}else {
		fmt.Println("查无此人")
	}

	// 映射元素删除
	delete(dict2,"xwb")

	fmt.Println(dict2["xwb"])
}
