s.boot;


(
var lowOctLevel, highOctLevel, dryOctLevel, btnOctLow, btnOctHigh, btnDry, setPoly;
var column1, column2, column3;
var t1, t2, t3, t4, t5, t6;
var amp;

var lowOctEffect, highOctEffect, dryOctEffect;
var dispertionLowOct, dispertionLow2Oct, dispertionHiOct, dispertionHi2Oct;
var w = Window.new("GUI Introduction", Rect(200,200,1000,300));
w.background=Color.grey(grey:0.9, alpha:0.6);
w.front;


//columns

column1 = CompositeView.new(w,bounds:Rect(220, 5,100,25));
StaticText.new(column1,Rect(0,0,150,30)).string_("one octave below");

column2 = CompositeView.new(w,bounds:Rect(400, 5,100,25));
StaticText.new(column2,Rect(0,0,150,30)).string_("dry signal");

column3 = CompositeView.new(w,bounds:Rect(580, 5,100,25));
StaticText.new(column3,Rect(0,0,150,30)).string_("one octave upon");

//button oct 1 high



btnOctLow = Button(parent:w, bounds:Rect(260, 30, 30, 30)).states_([
	["ON", Color.black, Color.green(val:0.7, alpha:0.5)],
    ["OFF", Color.white, Color.red(val:0.6, alpha:0.5)]
        ]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==1,
		{lowOctLevel.background_(Color.red(val:0.8, alpha:0.2)) && lowOctEffect.background_(Color.yellow(val:0.65, alpha:0.01)) },
		{lowOctLevel.background_(Color.red(val:0.8, alpha:0.5))  && lowOctEffect.background_(Color.yellow(val:0.95, alpha:0.01))});

});



btnDry = Button(parent:w, bounds:Rect(440, 30, 30, 30)).states_([
	["ON", Color.black, Color.green(val:0.7, alpha:0.5)],
    ["OFF", Color.white, Color.red(val:0.6, alpha:0.5)]
        ]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==1,
		{dryOctLevel.background_(Color.red(val:0.8, alpha:0.2)) && dryOctEffect.background_(Color.yellow(val:0.65, alpha:0.01))},
		{dryOctLevel.background_(Color.red(val:0.8, alpha:0.5)) && dryOctEffect.background_(Color.yellow(val:0.95, alpha:0.01))});

        });

btnOctHigh = Button(parent:w, bounds:Rect(620, 30, 30, 30)).states_([
	["ON", Color.black, Color.green(val:0.7, alpha:0.5)],
    ["OFF", Color.white, Color.red(val:0.6, alpha:0.5)]
        ]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==1,
		{highOctLevel.background_(Color.red(val:0.8, alpha:0.2)) && highOctEffect.background_(Color.yellow(val:0.65, alpha:0.01)) },
		{highOctLevel.background_(Color.red(val:0.8, alpha:0.5)) && highOctEffect.background_(Color.yellow(val:0.95, alpha:0.01))});

        });



//volume sliders, low, high, original
//Sliders are from 0 to 1 by default

lowOctLevel = Knob.new(w,Rect(220,90,110,30)).background_(Color.red(val:0.8, alpha:0.5));
t1 = CompositeView.new(w,Rect(265,60,200,30));
StaticText.new(t1,Rect(0,0,150,30)).string_("vol");
lowOctLevel.action_({
	arg knob;
  z.set(\octaveDown1KnobLevel, knob.value);

});


dryOctLevel = Knob.new(w,Rect(400,90,110,30)).background_(Color.red(val:0.8, alpha:0.5));
t2 = CompositeView.new(w,Rect(445,60,200,30));
StaticText.new(t2,Rect(0,0,150,30)).string_("vol");
/*highOctLevel.action_({
	octaveUp1KnobLevel=highOctLevel.value*100;

	totalLevel = dryKnobLevel + octaveUp1KnobLevel
	+ octaveUp2KnobLevel;

	octaveUp1Normalized=octaveUp1KnobLevel/totalLevel;

	x.set(\octaveUp1Bus, inputSignal*octaveUp1Normalized);
});
*/


highOctLevel = Knob.new(w,Rect(580,90,110,30)).background_(Color.red(val:0.8, alpha:0.5));
t3 = CompositeView.new(w,Rect(625,60,200,30));
StaticText.new(t3,Rect(0,0,150,30)).string_("vol");
highOctLevel.action_({
	//dryKnobLevel=dryKnobVol.value*100


});


//sliders for effect
// effect to write yet

lowOctEffect = Slider.new(w,Rect(260,180,30,100)).knobColor_(Color.red(val:0.6, alpha:1)).background_(Color.yellow(val:0.95, alpha:0.01));
t4 = CompositeView.new(w,Rect(260,160,200,30));
StaticText.new(t4,Rect(0,0,150,30)).string_("effect1 ");

dryOctEffect = Slider.new(w,Rect(440,180,30,100)).knobColor_(Color.red(val:0.6, alpha:1)).background_(Color.yellow(val:0.95, alpha:0.01));
t5 = CompositeView.new(w,Rect(440,160,200,30));
StaticText.new(t5,Rect(0,0,150,30)).string_("effect1");
dryOctEffect.action({

		//x.set(\timedelay, highOctDelay.value); where timeDelay will be a value in the delay effect associated to the synth x

	});

//x.set(\timedelay, highOctDelay.value); where timeDelay will be a value in the delay effect associated to the synth x


highOctEffect = Slider.new(w,Rect(620,180,30,100)).knobColor_(Color.red(val:0.6, alpha:1)).background_(Color.yellow(val:0.95, alpha:0.01));
t6 = CompositeView.new(w,Rect(620,160,200,30));
StaticText.new(t6,Rect(0,0,150,30)).string_("effect1");
highOctEffect.action_({

	});




//knob for the total volume
//name of variable to be defined
g = ControlSpec.new(0, 1, \lin);
amp = EZKnob(parent:w, bounds:Rect(730,150,100,100), label:"volume", controlSpec:g,  initVal:0.5 );
amp.setColors(Color.grey(grey:0.8, alpha:0.95), nil, Color.grey(grey:0.8, alpha:0.95));
amp.action_({

	//totAmp=amp.value
});



//button POLYPHONIC MONOPHONIC
setPoly = Button(parent:w, bounds:Rect(730, 50, 80, 30)).states_([
	["polyphonic", Color.black, Color.green(val:0.7, alpha:0.5)],
    ["monophonic", Color.white, Color.red(val:0.6, alpha:0.5)]
]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==1,
		{ w.background_(Color.grey(grey:0.5, alpha:0.92)) },
		{w.background_(Color.grey(grey:0.95, alpha:0.5))})
});


)



