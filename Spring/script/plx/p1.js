const p1Tag = "[plx/p1.js_v0.244]";
const btn4p1 = bl$("plx_p1_btn");

if(btn4p1){
    btn4p1.onclick = function(_this){ 
        var t = null;
        var md = null;
        return function(){
           if(!t){
            var t = new CTest();
            md = t.run();
           }
           blon(_this,md,"grey","green");
        }
    }(btn4p1);
} 




function CTest(){
    var md = null;
    var x = screen.width*0.19;
    var y = screen.height*0.15;
    var w =  screen.width*0.5;
    var h = screen.height*0.15;
    var to = {};
    to.playground = function(b,v){
        var c = "white";
        if(!md.playground)
        {
            md.playground = blo0.blDiv(md,md.id+"playground","playground",c); 

            var v = blo0.blDiv(md.playground,md.playground.id+"v","v",blGrey[3]);

            var cvs = blo0.blCanvase(v,w,444,"grey");      
            blo0.blText(cvs,"test",55,55,30,"green");          
            b.b = false;                
        } 
        blon(b,md.playground,blGrey[0],c); 
    }   
    this.run = function(){
        var cs = blGrey;
        md = blo0.blMD("id_md", p1Tag ,x,y,w,h,cs[0]);
        md.tb = blo0.blDiv(md,md.id+"tb","md.tb",cs[1]); 
        md.v = blo0.blDiv(md,md.id+"v","md.v",cs[2]); 
        var n = 0;
        for(i in to){
            var b = blo0.blBtn(md.tb,md.tb.id+"_btn_"+ i, i,cs[n]);
            b.style.float = "left";
            b.onclick = function(_this,_v,_i){
                return function(){
                    to[_i](_this,_v);
                }
            }(b,md.v,i);
            n++;
        } 
        return md;
    }
}