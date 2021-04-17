var fs = require( "fs" );

let fileReadStream = fs.createReadStream( "hello2.txt");

fileReadStream.on( "open", function ( fd ) {
    console.log( "文件被打开，文件句柄为%d", fd );
} );

// fileReadStream.pause();
// setTimeout( function () {
//     fileReadStream.resume();
// }, 2000 );

fileReadStream.on( "data", function ( dataChunk ) {
    console.log( "读取到数据：" );
    console.log( dataChunk.toString() );
} );

fileReadStream.on( "end", function () {
    console.log( "文件已经全部读取完毕" );
} );

fileReadStream.on( "close", function () {
    console.log( "文件被关闭" );
} );

fileReadStream.on( "error", function ( err ) {
    console.log( "文件读取失败。" );
} )