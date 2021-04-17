var fs=require("fs");

var ws=fs.createWriteStream("hello2.txt");

// 监听流的启停,使用once进行一次事件绑定
ws.once("open",function(){console.log("start");});
ws.once("closed",function(){console.log("stop");});

ws.write("今天天气真不错");
ws.write("锄禾日当午");
ws.write("红掌拨清清");

ws.close();
console.log("write over.");
