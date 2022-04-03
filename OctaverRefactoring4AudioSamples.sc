
/* Server and Input-Output setup */
(

Server.default=s=Server.local;

//Setup according to your system



s.options.inDevice_("Scarlett 2i2 USB");

s.options.outDevice_("Scarlett 2i2 USB");

s.options.hardwareBufferSize_(512);
s.options.sampleRate_(44100);


s.boot;

)


/* Octaver */


(




SynthDef(\OctaverMain,{

	arg inBus, outBus = 0,
	octaveUpBus,
	octaveDownBus,
	wet = 0,
	up = 0,
	volume = 0;

	var inputSignal;


	inputSignal = In.ar(inBus, 1);

	//Route the signal to the busses proportionally

	Out.ar(outBus, inputSignal*(1-wet)*volume!2);
	Out.ar(octaveUpBus, inputSignal*wet*up*volume);
	Out.ar(octaveDownBus, inputSignal*wet*(1-up)*volume);

}).add;



/* Pitch shifting following the "analog approach" */


SynthDef("OctaveUpMonophonic", {
	arg outBus, inBus;
	var lpfOut, rectSig, in;

	in = In.ar(inBus, 1);
	//We perform full wave rectification (by taking absolute value)
	//to double the frequency
	rectSig =abs(in);
	//Remove the DC component introduced by rectification
	rectSig = LeakDC.ar(rectSig, 0.999);
	//Lowpass the signal to smooth out the abrupt changes introduced by rectification
	//(This needs some manual tuning I think)



	Out.ar(outBus, rectSig*2);


}).add;




SynthDef("OctaveDownMonophonic", { arg outBus, inBus;
	var lpfOut, ff1, in, halfRect;
	in = In.ar(inBus, 1);



	//Do half-wave rectification

	halfRect = (in+abs(in))/2;
	ff1 = ToggleFF.ar(halfRect)-0.5; // use flip-flop to generate square wave an octave below, remove DC component

	Out.ar(outBus,ff1*halfRect);

}).add;




/* Phase vocoder pitch shifting approach */




SynthDef(\OctaveUpPolyphonic, {

	arg chorusBus, inBus, pitchShiftAmount = 2,
	fftSize = 8192, winLen = 4096, overlap=0.25, inWinType = 0;

	var in, chain;


	in = In.ar(inBus, 1);


	chain = FFT(LocalBuf(fftSize), in, overlap, inWinType, 1, winLen);

	//Shift bins
	chain = PV_PhaseShift(PV_MagShift(chain,pitchShiftAmount,0), pitchShiftAmount, 0);


	chain = IFFT(chain);
	Out.ar(chorusBus, chain);



}).add;

SynthDef(\OctaveDownPolyphonic, {


	arg chorusBus, inBus, pitchShiftAmount = 0.5,
	fftSize = 8192, winLen = 4096, overlap=0.25, inWinType = 0;

	var in, chain;

	in = In.ar(inBus, 1);

	chain = FFT(LocalBuf(fftSize), in, overlap, inWinType, 1, winLen);

	//Shift bins
	chain = PV_PhaseShift(PV_MagShift(chain,pitchShiftAmount,0), pitchShiftAmount, 0);

	chain = IFFT(chain);
	Out.ar(chorusBus, chain);

}).add;



/* Chorus SynthDef */


SynthDef(\Chorus, { arg inBus=10, outbus=0, predelay=0.08, rate=0.05, depth=0.015, ph_diff=0.5, wet = 0;
	var in, sig, modulators, numDelays = 12, source;


	//Depth must range between 0.015 and 0.035 (milliseconds of delay)

	source = In.ar(inBus, 1);
	in = source * numDelays.reciprocal;
	modulators = Array.fill(numDelays, {arg i; LFPar.kr(rate*rrand(0.94, 1.06), ph_diff * i, depth, predelay);});
	sig = DelayC.ar(in, 0.5, modulators);
	sig = sig.sum;
	Out.ar(outbus, wet*sig!2);
	Out.ar(outbus, (1-wet)*source!2);
}).add;





SynthDef(\readInputSignal, {

	arg outBus = 0, mode = 1,buf = 0, amp = 1, rate = 1, spos = 0, pan = 0;

	//Adjust argument to your input port
	//Out.ar(outBus, SoundIn.ar(1));

	//Out.ar(outBus, SinOsc.ar(440, 0, 0.5));

	var sig;
	sig = PlayBuf.ar(1,buf,rate, startPos: spos, doneAction:2);
	sig = Pan2.ar(sig,pan,amp);
	Out.ar(outBus,sig);

}).add;


SynthDef(\lowPass, {
	arg outBus, inBus, lp = 200;
	var lpfOut, in;

	in = In.ar(inBus, 1);
	lpfOut = LPF.ar(in,lp);

	Out.ar(outBus,lpfOut!2);
}).add;


)





(

//Audio bus variables

var t1, t2, t3, t4,t5;
var octaveUp1BusMonophonic = Bus.audio(s,1);
var octaveDown1BusMonophonic = Bus.audio(s,1);

var octaveUp1BusPolyphonic = Bus.audio(s,1);
var octaveDown1BusPolyphonic = Bus.audio(s,1);
var lpBus = Bus.audio(s, 1);
var chorusBus = Bus.audio(s, 1); //Bus for chorus FX


var inputBus = Bus.audio(s, 1);

//Synth def variables

var octUp1MonoSD, octUp1PolySD, octDown1MonoSD, octDown1PolySD, chorusSD,lowPassSD;



//GUI variables



var octaveKnob, wetKnob, chorusKnob, setPoly;
var chorusDepth, chorusRate;
var ampKnob, lpfKnob;

var depthCS, rateCS, lpfCS;


var w = Window.new("DelayLama Octaver", Rect(200,200,800,450));

var v = UserView(w, Rect(0,0,800,450));



var b;
b = Buffer.read(s,thisProcess.nowExecutingPath.dirname +/+ "try2.wav");

//Bus and SynthDef setup

g = Group.new;

octUp1PolySD = Synth(\OctaveUpPolyphonic, [\inBus, octaveUp1BusPolyphonic,
	\chorusBus, chorusBus], g);
octDown1PolySD = Synth(\OctaveDownPolyphonic, [\inBus, octaveDown1BusPolyphonic,
	\chorusBus, chorusBus], g);
octDown1MonoSD = Synth(\OctaveDownMonophonic, [\inBus, octaveDown1BusMonophonic,
	\outBus, lpBus], g);
octUp1MonoSD = Synth(\OctaveUpMonophonic, [\inBus, octaveUp1BusMonophonic,
	\outBus, lpBus], g);

z = Synth.before(g,\OctaverMain, [\inBus, inputBus, \octaveUpBus, octaveUp1BusMonophonic,
	\octaveDownBus, octaveDown1BusMonophonic]);

lowPassSD = Synth.after(g,\lowPass, [\inBus, lpBus,
	\outBus, chorusBus]);

chorusSD = Synth.after(lowPassSD, \Chorus, [\inBus, chorusBus]);


h = Synth.before(z,\readInputSignal, [\outBus, inputBus, \mode, 1, \buf,b]);



//GUI setup


w.front;




i = Image.open(thisProcess.nowExecutingPath.dirname +/+ "octaver2.png");
i.plot();
i.url.postln;


v.backgroundImage_(i);

v.front;
v.visible = true;
v.alwaysOnTop = true;
v.resize = 1;




octaveKnob = Knob.new(v,Rect(72,160,70,70)).background_(Color.blue(val:0.8, alpha:0.5));
//t1 = CompositeView.new(w,Rect(265,60,200,30));
//StaticText.new(t1,Rect(0,0,150,30)).string_("Octave Down/Up");
octaveKnob.action_({
	arg knob;
  z.set(\up, knob.value);
	knob.value.postln;

});

octaveKnob.mouseEnterAction_({
	arg knob;
	t2 = StaticText.new(v, Rect(knob.bounds.left,
		knob.bounds.top-90,knob.bounds.width,knob.bounds.height)).align_(\center)
	.string_("Octave down/up").stringColor_(Color.grey(1,1)).font_(Font("Serif",16));
});
octaveKnob.mouseLeaveAction_({
	t2.remove;
});

wetKnob = Knob.new(v,Rect(117,260,70,70)).background_(Color.blue(val:0.8, alpha:0.5));
//t1 = CompositeView.new(w,Rect(265,60,200,30));
//StaticText.new(t1,Rect(0,0,150,30)).string_("Octave Down/Up");
wetKnob.action_({
	arg knob;
  z.set(\wet, knob.value);
	knob.value.postln;

});


wetKnob.mouseEnterAction_({
	arg knob;
	t3 = StaticText.new(v, Rect(knob.bounds.left,
		knob.bounds.top+90,knob.bounds.width,knob.bounds.height)).align_(\center)
	.string_("Octave dry/wet").stringColor_(Color.grey(1,1)).font_(Font("Serif",16));
});
wetKnob.mouseLeaveAction_({
	t3.remove;
});

ampKnob = Knob.new(v,Rect(350,275,130,130)).background_(Color.blue(val:0.8, alpha:0.5));
//t1 = CompositeView.new(w,Rect(265,60,200,30));
//StaticText.new(t1,Rect(0,0,150,30)).string_("Octave Down/Up");
ampKnob.action_({
	arg knob;
  z.set(\volume, knob.value);
	knob.value.postln;

});

ampKnob.mouseEnterAction_({
	arg knob;
	t1 = StaticText.new(v, Rect(knob.bounds.left,
		knob.bounds.top+70,knob.bounds.width,knob.bounds.height)).align_(\center)
	.string_("Output volume").stringColor_(Color.grey(1,1)).font_(Font("Serif",16));
});
ampKnob.mouseLeaveAction_({
	t1.remove;
});

chorusKnob = Knob.new(v,Rect(536,360,55,55)).background_(Color.blue(val:0.8, alpha:0.5));
//t1 = CompositeView.new(w,Rect(265,60,200,30));
//StaticText.new(t1,Rect(0,0,150,30)).string_("Octave Down/Up");
chorusKnob.action_({
	arg knob;
  chorusSD.set(\wet, knob.value);
	knob.value.postln;

});
StaticText(v, Rect(536,400,55,55)).string_('Wet').stringColor_(Color.grey(1,1)).font_(Font("Serif",16))
.align_(\center);


depthCS = ControlSpec(0.015, 0.05);
chorusDepth = Knob(v,Rect(620,360,55,55)).background_(Color.blue(val:0.8, alpha:0.5));
chorusDepth.action_({
	arg knob;
	var mappedDepth;

	mappedDepth = depthCS.map(knob.value);
  chorusSD.set(\depth, mappedDepth);
	mappedDepth.postln;

});


StaticText(v, Rect(620,400,55,55)).string_('Depth').stringColor_(Color.grey(1,1)).font_(Font("Serif",16))
.align_(\center);


rateCS = ControlSpec(0.05, 0.1);

chorusRate = Knob.new(v,Rect(704,360,55,55)).background_(Color.blue(val:0.8, alpha:0.5));
chorusRate.action_({
	arg knob;
	var mappedRate;

	mappedRate = rateCS.map(knob.value);

  chorusSD.set(\rate, mappedRate);
	mappedRate.postln;

});



StaticText(v, Rect(704,400,55,55)).string_('Rate').stringColor_(Color.grey(1,1)).font_(Font("Serif",16))
.align_(\center);


lpfCS = ControlSpec(200, 8000,'exp');
lpfKnob = Knob.new(v,Rect(241,313,55,55)).background_(Color.blue(val:0.8, alpha:0.5));

//t1 = CompositeView.new(w,Rect(265,60,200,30));
//StaticText.new(t1,Rect(0,0,150,30)).string_("Octave Down/Up");
lpfKnob.action_({
	arg knob;
	var mappedLpf;
	mappedLpf = lpfCS.map(knob.value);
	lowPassSD.set(\lp, mappedLpf);
	knob.value.postln;

});
lpfKnob.mouseEnterAction_({
	arg knob;
	t5 = StaticText.new(v, Rect(knob.bounds.left,
		knob.bounds.top + 55 ,knob.bounds.width,knob.bounds.height)).align_(\center)
	.string_("LPF Cutoff").stringColor_(Color.grey(1,1)).font_(Font("Serif",16));
});
lpfKnob.mouseLeaveAction_({
	t5.remove;
});


//button POLYPHONIC MONOPHONIC
setPoly = Button(parent:v, bounds:Rect(231, 273, 80, 30)).states_([
	["monophonic", Color.black, Color.green(val:0.7, alpha:0.5)],
    ["polyphonic", Color.white, Color.red(val:0.6, alpha:0.5)]
]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==1,
		{
			w.background_(Color.grey(grey:0.5, alpha:0.92));
			//Switch busses to polyphonic
			z.set(\octaveUpBus, octaveUp1BusPolyphonic);
			z.set(\octaveDownBus, octaveDown1BusPolyphonic);
			lpfKnob.visible = 0;

		},
		{
			w.background_(Color.grey(grey:0.95, alpha:0.5));
			//Switch busses to monophonic
			z.set(\octaveUpBus, octaveUp1BusMonophonic);
			z.set(\octaveDownBus, octaveDown1BusMonophonic);
			lpfKnob.visible = 1;
		}
	)
});


w.onClose_({
	s.quit;
});
)
b = Buffer.read(s,thisProcess.nowExecutingPath.dirname +/+ "try2.wav");
