var fs=require("fs");

var fd=fs.openSync("hello.txt","w");
var fd1=fs.openSync("hello1.txt","w");

fs.writeSync(fd,"lollipop",0);

fs.closeSync(fd);

console.log("write over.")

fs.writeFile(
    "callback.txt",
    "this is callback written in text.",
    {flag:"w"},
    function(err){
        if(!err){
            console.log("written success -_-.")
        }else{
            console.log(err);
        }
    }
);

var path="callback.txt";
fs.readFile(
    path,
    function(err,data){
        if(!err){
            console.log(data.toString());
        }else{
            console.log(err);
        }
    }
)

fs.write(
    fd1,
    "This is async write result.",
    function(err){
        if(!err)
            console.log("write success");
        fs.close(
            fd1,
            function(err){
                if(!err){
                    console.log("file is closed.")
                }
            }
        )
    }
)