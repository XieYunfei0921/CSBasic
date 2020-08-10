package main

import (
	"fmt"
	"math/rand"
	"sync"
	"time"
)

const(
	numberGroutines=4
	taskLoad=10
)

var wg7 sync.WaitGroup

func worker(tasks chan string,worker int){
	defer wg7.Done()
	for {
		task,ok:=<-tasks
		if !ok {
			fmt.Printf("Worker: %d : Shutting Down\n",worker)
			return
		}
		// 显示工作信息
		fmt.Printf("Worker: %d : Started %s\n",worker,task)
		sleep:=rand.Int63n(100)
		time.Sleep(time.Duration(sleep)*time.Millisecond)
		fmt.Printf("Worker: %d : Completed %s\n",worker,task)
	}
}

func init()  {
	rand.Seed(time.Now().UnixNano())
}

func main() {
	tasks:=make(chan string,taskLoad)
	wg7.Add(numberGroutines)
	for gr:=1;gr<=numberGroutines ;gr++{
		go worker(tasks,gr)
	}

	for post := 1; post<=taskLoad;post++{
		// 将task n送入任务字段
		tasks<-fmt.Sprintf("Task: %d",post)
	}

	close(tasks)
	wg7.Wait()
}
