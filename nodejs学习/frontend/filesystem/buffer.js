
var buff=Buffer.from("lollipop");
console.log(buff.toString());

var buff1=Buffer.alloc(4);
buff1[0]=0x6c;
buff1[1]=0x6f;
buff1[2]=0x6c;
buff1[3]=0x69;
console.log(buff1.toString());