/**
 * Node中,一个js文件就是一个模块
 * 每个js文件中的js代码都是独立运行在一个函数中的,而不是全局作用域,
 * 所以一个模块中的变量和函数无法在其他模块中无法访问,即闭包的概念
 */
console.log("i am a module, i am module1.js")

/**
 * 通过exports向外部暴露变量和方法
 * 相应属性和方法作为exports成员变量申明即可
 */

exports.x="i am in module1.js";
exports.y="i am y";

exports.fn=function(){

}