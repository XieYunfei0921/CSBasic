#### let关键字

let 关键字用来声明变量，使用 let 声明的变量有几个特点：

1. 不允许重复声明
2. 块级作用域
3. 不存在变量提升
4. 不影响作用域链 

> 声明变量尽量使用`let`

#### `const` 关键字

const 关键字用来声明常量， const 声明有以下特点

1. 声明必须赋初始值
2. 标识符一般为大写
3. 不允许重复声明
4. 值不允许修改 
5. 块级作用域 

应用场景：声明对象类型使用 `const`，非对象类型声明选择 `let`. 对象属性修改和数组元素变化不会出发 const 错误  

#### 模板字符串

模板字符串（template string）是增强版的字符串， 用反引号（`）标识，特点：

1. 字符串中可以出现换行符
2. 可以使用 `${xxx}` 形式输出变量 

#### 箭头函数 

`ES6` 允许使用「箭头」 （=>）定义函数.  

```javascript
let fn = (arg1, arg2, arg3) => {
	return arg1 + arg2 + arg3;
}
```



1. 如果形参只有一个，则小括号可以省略
2. 函数体如果只有一条语句，则花括号可以省略，函数的返回值为该条语句的执行结果
3. 箭头函数 this 指向声明时所在作用域下 this 的值
4. 箭头函数不能作为构造函数实例化
5. 不能使用 arguments 

#### rest 参数

`ES6` 引入 `rest` 参数，用于获取函数的实参，用来代替 `arguments` 

```javascript
function add(...args){
	console.log(args);
}
add(1,2,3,4,5);
```

#### spread 扩展运算符

扩展运算符（`spread`）也是三个点（`...`）. 它好比 rest 参数的逆运算，将一个数组转为用逗号分隔的参数序列，对数组进行解包.  

#### Symbol对象

`ES6` 引入了一种新的原始数据类型 `Symbol`，表示独一无二的值. 它是`JavaScript` 语言的第七种数据类型，是一种类似于字符串的数据类型. 
`Symbol` 特点:

1. `Symbol` 的值是唯一的，用来解决命名冲突的问题
2. `Symbol` 值不能与其他数据进行运算
3. `Symbol` 定义 的 对象属 性 不能 使 用 for…in 循 环遍 历 ，但 是可 以 使 用`Reflect.ownKeys` 来获取对象的所有键名 

#### 生成器

生成器函数是 `ES6` 提供的一种异步编程解决方案，语法行为与传统函数完全不同 

```javascript
function * gen(){
    yield '一只没有耳朵';
    yield '一只没有尾巴';
    return '真奇怪';
}
let iterator = gen();
console.log(iterator.next());
console.log(iterator.next());
console.log(iterator.next());
```

#### promise

`Promise` 是 `ES6` 引入的异步编程的新解决方案. 语法上 `Promise` 是一个构造函数，用来封装异步操作并可以获取其成功或失败的结果. 
1) `Promise` 构造函数: `Promise (excutor) {}`
2) `Promise.prototype.then` 方法
3) `Promise.prototype.catch` 方法 

#### class

`ES6` 提供了更接近传统语言的写法，引入了 `Class`（类）这个概念，作为对象的模板. 通过 `class` 关键字，可以定义类. 基本上， `ES6` 的 `class` 可以看作只是一个语法糖，它的绝大部分功能， `ES5` 都可以做到，新的 `class` 写法只是让对象原型的写法更加清晰、更像面向对象编程的语法而已.  

#### 模块化

模块化的优势有以下几点：
1) 防止命名冲突
2) 代码复用
3) 高维护性 

模块功能主要由两个命令构成： export 和 import。

+ export 命令用于规定模块的对外接口
+  import 命令用于输入其他模块提供的功能 