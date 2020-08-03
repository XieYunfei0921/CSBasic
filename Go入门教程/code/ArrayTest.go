package main

import "fmt"

func main() {

	var arr1 [5]int
	arr1[2]=35

	for ele := range arr1 {
		fmt.Println(arr1[ele])
	}


	arr2 :=[5]int{10,20,30,40,50}
	for k, v := range arr2 {
		fmt.Println(k," => ",v)
	}

	arr3 := [5]int{1:20,2:10}
	arr3[3]=17

	// 指针数组的初始化
	arr4 := [5]*int{0:new(int),1:new(int)}
	*arr4[0]=12
	*arr4[1]=31

	for k, v := range arr4 {
		if v!=nil {
			fmt.Println(k," => ",*v)
		}
	}

	/*指针数组的复制*/
	var array1 [3]*string
	_ = array1
	array2 :=[3]*string{new(string),new(string),new(string)}
	*array2[0]="Red"
	*array2[1]="Blue"
	*array2[2]="Green"
	array1=array2
	for k, v := range array2 {
		fmt.Println(k," => ",*v)
	}

	for k,v := range array1{
		fmt.Println(k," => ",*v)
	}

	arr5 := [4][2]int{{10, 11}, {20, 21}, {30, 31}, {40, 41}}
	arr5[2][0]=7

	for k, v := range arr5 {
		fmt.Println(k," => ",v)
	}

}
