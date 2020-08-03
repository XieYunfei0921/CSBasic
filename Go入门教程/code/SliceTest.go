package main

import "fmt"

/**
	切片测试
 */
func main() {
	// 3是长度,5是容量，创建之后并不是所有的元素都可以访问
	slices:= make([]string,3,5)
	slices[1]="sandee"
	slices[2]="offer"
	for e := range slices {
		fmt.Println(slices[e])
	}

	// 如果长度大于容量,则会抛出异常
	//slices2:= make([]int,5,3)
	//for e := range slices2 {
	//	fmt.Println(slices2[e])
	//}

	fmt.Println("---空切片声明1---")
	// 只声明而不赋值,则为空切片
	var slices3 []int
	for e := range slices3 {
		fmt.Println(slices3[e])
	}

	fmt.Println("---空切片声明2---")
	// 空切片声明方式2
	slices4:= []int{}
	for e := range slices4 {
		fmt.Println(slices4[e])
	}

	fmt.Println("---切片字面量声明---")
	slices5:=[]int{31,22,44,12,32}
	for e := range slices5 {
		fmt.Println(slices5[e])
	}

	fmt.Println("---使用切片创建切片---")
	// 范围为[leftIndex,rightIndex)
	slices6:=slices5[1:3]
	for e := range slices6 {
		fmt.Println(slices6[e])
	}

	fmt.Println("切片值的修改")
	/**
	从修改的结果上来看,这两个切片相应位置的值都发生了变化,修改是发生在对应的寄存器上的.
	且这两个index都是指向了这个位置,所以值会同时发生变化
	 */
	slices6[0]=99
	// 原切片
	for e := range slices5 {
		fmt.Println(slices5[e])
	}
	fmt.Println("&&&&&&")
	// 复制切片
	for e := range slices6 {
		fmt.Println(slices6[e])
	}

	fmt.Println("----使用append实现切片的增长----")
	// append会增长切片的长度,可能会改变切片的容量(当容量不足的时候)
	slices7 := append(slices6, 77)
	for e := range slices7 {
		fmt.Println(slices7[e])
	}

	fmt.Println("-------切片的容量控制-------")
	source:=[]string{"Apple","Orange","Banana","Grape"}
	/**
	slice[i:j:k]
	长度: j-i
	容量: k-i
	 */
	slices8:=source[1:2:2]
	for e := range slices8 {
		fmt.Println(slices8[e])
	}
	// 当容量=长度的时候,再添加元素就会报错
	slices9 := append(slices8, "sandee")
	// 检查添加结果,添加成功,而slice8还是一个元素,所以保证了slice8的安全
	fmt.Println(slices9[0])
	fmt.Println(slices9[1])
	slices9[0]="peer"
	fmt.Println(slices9[0])
	fmt.Println("------------------------")
	// 访问一下slice[2], 看一看底层是否发生变化,修改9的元素,发现slice8,9是两组数组空间
	// 这个结论仅仅会在容量==长度时成立,否则都会被修改
	fmt.Println(slices8[0])
	
	// 传统迭代
	for index:=1;index<len(source) ;index++  {
		fmt.Printf("Index %d Value: %s\n",index,source[index])
	}
}
