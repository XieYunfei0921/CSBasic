package main

import (
	"fmt"
	"math/rand"
	"sync"
	"time"
)

var wg6=sync.WaitGroup{}

func init()  {
	rand.Seed(time.Now().UnixNano())
}

func player(name string, court chan int){
	defer wg6.Done()
	for {
		ball,ok:=<-court
		if !ok {
			// 通道关闭,就算胜利
			fmt.Printf("Player %s Won\n",name)
			return
		}
		n:=rand.Intn(100)
		if n%13==0{
			fmt.Printf("Player %s Missed\n",name)
			close(court)
			return
		}
		fmt.Printf("Player %s Hit %d\n",name,ball)
		ball++;
		// 将求打向对手
		court<-ball
	}
}

func main() {
	court:=make(chan int)
	wg6.Add(2)
	go player("Sandee",court)
	go player("Wsd",court)
	court<-1
	wg6.Wait()
}
