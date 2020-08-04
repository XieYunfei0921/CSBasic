package main

import "fmt"

/**
	用户类型定义
 */
type user struct {
	name string
	email string
	ext int
	privileged bool
}

// 类型复合
type admin struct {
	person user
	level int
}

// 接口定义
type notifier interface {
	notify()
}

// 申明已经存在的类型
type duration int64

/**
	func关键字与函数标识符之间的叫做函数的接收者, 即函数调用者的类型申明
 */
func (u *user) notify() {
	fmt.Printf("Sending Email to %s with email %s",u.name,u.email)
}

func (u *user) changeEmail(email string) {
	u.email=email
}

func sendNotification(n notifier)  {
	n.notify()
}

func (d duration) pretty() string {
	return fmt.Sprintf("Duration: %d",d)
}

func main() {
	sandee:=user{
		name:	"sandee",
		email:	"2511006068@qq.com",
		ext:	7,
		privileged:	true,
	}
	mxd:=user{"mxd","1127485932@qq.com",6,false}
	fmt.Println(sandee,"\n",mxd)

	cindy:=&user{"cindy","wsd19940825@126.com",8,false}

	admin1:=admin{
		person: sandee,
		level:	7,
	}
	fmt.Println(admin1)

	fmt.Println("--------------------------")
	sandee.notify()
	fmt.Println()
	cindy.notify()
	fmt.Println()

	sandee.changeEmail("sandee@gmail.com")
	sandee.notify()

	fmt.Println("\n--------------------------")
	/**
	使用指针接收者的时候, 提示user没有实现notifier接口
	这里介绍一下方法集:
	方法集定义了一组关联到值/指针的方法,接受者的类型决定了是关联到值还是指针,还是都关联

	方法接受者	值
	(t T)		T和*T
	(t *T)		*T

	也就是说,当使用指针接受者的时候,只有指向这个类型的指针才能实现对应的接口
	使用值接受者的时候,类型对应的值和指针都可以实现接口
	 */
 	fmt.Println("---------多态测试---------")
	sendNotification(cindy)
 	fmt.Println()
	sendNotification(&sandee)

	/**
	值方法集
	值	方法接受者
	T	(t T)
	*T	(t T)/(t *T)
	因为总是不能获取一个值的地址(传入指针就表示是地址), 所以值的方法集只包含使用值接受者的方法
	 */
	duration(42).pretty()
}
