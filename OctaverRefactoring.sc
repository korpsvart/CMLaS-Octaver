
/* Server and Input-Output setup */
(

Server.default=s=Server.local;

//Setup according to your system



s.options.inDevice_("Analog (1+2) (RME Babyface)");

s.options.outDevice_("Altoparlanti (RME Babyface");

s.options.hardwareBufferSize_(512);
s.options.sampleRate_(48000);


s.boot;

)


/* Utility functions */





/* Octaver */


(




SynthDef(\octaverMain,{



	arg inBus, outBus = 0,
	octaveUpBus,
	octaveDownBus,
	wet = 0,
	up = 0,
	volume = 0;

	var inputSignal;


	inputSignal = In.ar(inBus, 1);

	//Route the signal to the busses proportionally
	//to knob normalized values


	Out.ar(outBus, inputSignal*(1-wet)*volume);
	Out.ar(octaveUpBus, inputSignal*wet*up*volume);
	Out.ar(octaveDownBus, inputSignal*wet*(1-up)*volume);

}).add;


/*Pitch shifting in time domain (using PitchShift.ar) version */

SynthDef(\octaveUp1PitchShiftTimeDomain, {
	arg outBus = 0, inBus;

	var inputSource, pitchShifted;

	inputSource = In.ar(inBus, 1);

	pitchShifted = PitchShift.ar(inputSource, pitchRatio:2);

	Out.ar(outBus, pitchShifted);

}).add;


SynthDef(\octaveUp2PitchShiftTimeDomain, {
	arg outBus = 0, inBus;

	var inputSource, pitchShifted;

	inputSource = In.ar(inBus, 1);

	pitchShifted = PitchShift.ar(inputSource, pitchRatio:4);

	Out.ar(outBus, pitchShifted);

}).add;



/* Pitch shifting following the "analog approach" */


SynthDef("octaveUp1", {
	arg outBus=0, inBus;
	var lpfOut, rectSig, in, freq;

	in = In.ar(inBus, 1);
	//We perform full wave rectification (by taking absolute value)
	//to double the frequency
	rectSig =abs(in);
	//Remove the DC component introduced by rectification
	rectSig = LeakDC.ar(rectSig, 0.999);
	//Lowpass the signal to smooth out the abrupt changes introduced by rectification
	//(This needs some manual tuning I think)
	lpfOut = LPF.ar(rectSig, 4000);



	Out.ar(outBus, lpfOut*2);


}).add;




SynthDef("octaveDown1", { arg outBus=0, inBus;
	var lpfOut, ff1, ff2, rectSig, in, oct1, oct2, direct, halfRect;
	in = In.ar(inBus, 1);



	//Do half-wave rectification

	in = LPF.ar(in, 800);

	halfRect = (in+abs(in))/2;
	ff1 = ToggleFF.ar(halfRect)-0.5; // use flip-flop to generate square wave an octave below, remove DC component



	Out.ar(outBus, ff1*halfRect);
}).add;


/* Pitch Shift using pitch tracking (obviously monophonic) */



SynthDef("octaveUp1PT", { arg outBus=0, inBus, pitchShiftAmount = 2;
	var freq, in, source, env;
	in = In.ar(inBus, 1);



	//Do half-wave rectification

	freq = Pitch.kr(in);
	//env = Amplitude.kr(in);
	source = SinOsc.ar(freq*pitchShiftAmount, mul:0.2);

	Out.ar(outBus, source);
}).add;




/* Phase vocoder pitch shifting approach */


SynthDef(\phaseVocoderPitchShift,{
	arg outBus, inBus, pitchShiftAmount = 2;

	var in, chain;

	in = In.ar(inBus, 1);
	chain = FFT(LocalBuf(4096), in);
	chain = PV_MagShift(chain, pitchShiftAmount);
	chain = IFFT(chain);
	//chain = LPF.ar(chain, 8000);
	Out.ar(outBus, IFFT(chain));


}).add;


SynthDef(\phaseVocoderAdvanced, {
	arg inBus, outBus = 0;


	var in, out, amp, fftSize=8192, winLen=2048, overlap=0.5 ,
	chain, mexp, fScaled, df, binShift, phaseShift, inWinType=0, outWinType=0, f0,
	hasFreq;

	in = In.ar(inBus, 1);



	# f0, hasFreq = Pitch.kr(in);


	chain = FFT(LocalBuf(fftSize), in, overlap, inWinType, 1, winLen);

	fScaled = f0 * 2;
    df = fScaled - f0;
    binShift = fftSize * (df / s.sampleRate);
    chain = PV_BinShift(chain, stretch:1, shift:binShift, interp:1);
    phaseShift = 2 * pi * binShift * overlap * (winLen/fftSize);
    chain = PV_PhaseShift(chain, phaseShift, integrate:1);
    out = IFFT(chain,outWinType,winLen);
    Out.ar(outBus, out.dup);
}).add;



//Global variables
p = 0; //frame number counter

SynthDef(\phaseVocoderOCEANUp1, {


	arg outBus, inBus, pitchShiftAmount = 2,
	fftSize = 8192, winLen = 4096, overlap=0.25, inWinType = 0, outWinType = 0;

	var in, chain, multiplier, newBin;

	multiplier = (2*pi) / (overlap*fftSize);





	in = In.ar(inBus, 1);


	chain = FFT(LocalBuf(fftSize), in, overlap, inWinType, 1, winLen);

	//Shift bins
	chain = PV_BinShift(chain, pitchShiftAmount, 0);


	//Adjust phase


	chain = chain.pvcollect(1024,{ arg magnitude, phase, bin, index;
		//p = p + 1;

		[magnitude, phase + (pitchShiftAmount*bin-bin+0.5)*multiplier]
	});







	chain = IFFT(chain);
	//chain = LPF.ar(chain, 8000);
	Out.ar(outBus, chain);



}).add;

SynthDef(\phaseVocoderOCEANDown1, {


	arg outBus, inBus, pitchShiftAmount = 0.5,
	fftSize = 8192, winLen = 4096, overlap=0.25, inWinType = 0, outWinType = 0;

	var in, chain, multiplier, newBin;

	multiplier = (2*pi * p) / (overlap*fftSize);





	in = In.ar(inBus, 1);


	chain = FFT(LocalBuf(fftSize), in, overlap, inWinType, 1, winLen);

	//Shift bins
	chain = PV_MagShift(chain, pitchShiftAmount, 0);


	//Adjust phase

	/*

	chain = chain.pvcollect(1024,{ arg magnitude, phase, bin, index;
		p = p + 1;

		[magnitude, phase + (pitchShiftAmount*bin-bin+0.5)*multiplier]
	});

	*/





	chain = IFFT(chain);
	//chain = LPF.ar(chain, 8000);
	Out.ar(outBus, chain);



}).add;








SynthDef(\readInputSignal, {

	arg outBus = 0;

	//Adjust argument to your input port
	//Out.ar(outBus, SoundIn.ar(1));

	Out.ar(outBus, SinOsc.ar(440, 0, 0.5));

}).add;



)





(

//Audio bus variables


var octaveUp1BusMonophonic = Bus.audio(s,1);
var octaveDown1BusMonophonic = Bus.audio(s,1);

var octaveUp1BusPolyphonic = Bus.audio(s,1);
var octaveDown1BusPolyphonic = Bus.audio(s,1);


var inputBus = Bus.audio(s, 1);

//Synth def variables

var octUp1MonoSD, octUp1PolySD, octDown1MonoSD, octDown1PolySD;



//GUI variables



var octaveKnob, wetKnob, setPoly;
var ampKnob;


var w = Window.new("GUI Introduction", Rect(200,200,800,450));

var v = View(w, Rect(0,0,800,450));



//Bus and SynthDef setup

octUp1PolySD = Synth(\phaseVocoderOCEANUp1, [\inBus, octaveUp1BusPolyphonic]);
octDown1PolySD = Synth(\phaseVocoderOCEANDown1, [\inBus, octaveDown1BusPolyphonic]);
octDown1MonoSD = Synth(\octaveDown1, [\inBus, octaveDown1BusMonophonic]);
octUp1MonoSD = Synth(\octaveUp1, [\inBus, octaveUp1BusMonophonic]);
z = Synth(\octaverMain, [\inBus, inputBus, \octaveUpBus, octaveUp1BusMonophonic,
	\octaveDownBus, octaveDown1BusMonophonic,
]);


h = Synth(\readInputSignal, [\outBus, inputBus]);



//GUI setup


w.front;




i = Image.open(thisProcess.nowExecutingPath.dirname +/+ "octaver2.png");
//i.plot();
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

wetKnob = Knob.new(v,Rect(117,260,70,70)).background_(Color.blue(val:0.8, alpha:0.5));
//t1 = CompositeView.new(w,Rect(265,60,200,30));
//StaticText.new(t1,Rect(0,0,150,30)).string_("Octave Down/Up");
wetKnob.action_({
	arg knob;
  z.set(\wet, knob.value);
	knob.value.postln;

});

ampKnob = Knob.new(v,Rect(368,175,55,55)).background_(Color.blue(val:0.8, alpha:0.5));
//t1 = CompositeView.new(w,Rect(265,60,200,30));
//StaticText.new(t1,Rect(0,0,150,30)).string_("Octave Down/Up");
ampKnob.action_({
	arg knob;
  z.set(\volume, knob.value);
	knob.value.postln;

});




//button POLYPHONIC MONOPHONIC
setPoly = Button(parent:w, bounds:Rect(730, 50, 80, 30)).states_([
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

		},
		{
			w.background_(Color.grey(grey:0.95, alpha:0.5));
			//Switch busses to monophonic
			z.set(\octaveUpBus, octaveUp1BusMonophonic);
			z.set(\octaveDownBus, octaveDown1BusMonophonic);
		}
	)
});




)









