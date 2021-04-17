
/**
 * 核心模块:
 *  node自带,表示名称就是模块名
 * 文件模块
 *  用户自己创建的模块
 *  文件模块标识就是文件路径
 * 
 * 模块封装的时候,同时包括
 * 1. exports 向外暴露的属性
 * 2. require 外部引入
 * 3. module 当前模块本身
 * 4. __file_name 文件名称
 * 5. __dirname 目录名称
 */
var math=require("./math");
console.log(math.add(111,222));


var fs=require("fs");
console.log(fs);

console.log("filename -> "+__filename);
console.log("dirname -> "+__dirname);

console.log(module);