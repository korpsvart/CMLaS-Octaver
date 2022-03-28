
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

	//For simplicity I assume the knobs in the GUI will range
	//within [0, 100] but it's arbitrary

	arg inBus, outBus = 0,
	octaveUp1Bus, octaveUp2Bus,
	octaveDown1Bus, octaveDown2Bus,
	dryKnobLevel = 0,
	octaveUp1KnobLevel = 0,
	octaveUp2KnobLevel = 0,
	octaveDown1KnobLevel = 0,
	octaveDown2KnobLevel = 0;

	var inputSignal;
	var totalLevel, dryKnobNormalized,
	octaveUp1Normalized, octaveUp2Normalized,
	octaveDown1Normalized, octaveDown2Normalized;

	//Calculate signal proportions
	//to make sure the sum amplitude is always
	//constant (= 1)



	totalLevel = dryKnobLevel + octaveUp1KnobLevel
	+ octaveUp2KnobLevel +octaveDown1KnobLevel
	+ octaveDown2KnobLevel;

	dryKnobNormalized = dryKnobLevel / totalLevel;
	octaveUp1Normalized = octaveUp1KnobLevel / totalLevel;
	octaveUp2Normalized = octaveUp2KnobLevel / totalLevel;
	octaveDown1Normalized = octaveDown1KnobLevel / totalLevel;
	octaveDown2Normalized = octaveDown2KnobLevel / totalLevel;



	inputSignal = In.ar(inBus, 1);

	//Route the signal to the busses proportionally
	//to knob normalized values


	Out.ar(outBus, inputSignal*dryKnobNormalized);
	Out.ar(octaveUp1Bus, inputSignal*octaveUp1Normalized);
	Out.ar(octaveUp2Bus, inputSignal*octaveUp2Normalized);
	Out.ar(octaveDown1Bus, inputSignal*octaveDown1Normalized);
	Out.ar(octaveDown2Bus, inputSignal*octaveDown2Normalized);

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
	var lpfOut, rectSig, in;
	in = In.ar(inBus, 1);
	//We perform full wave rectification (by taking absolute value)
	//to double the frequency
	rectSig =abs(in);
	//Remove the DC component introduced by rectification
	rectSig = LeakDC.ar(rectSig, 0.999);
	//Lowpass the signal to smooth out the abrupt changes introduced by rectification
	//(This needs some manual tuning I think)
	lpfOut = LPF.ar(rectSig, 4000);


	Out.ar(outBus, lpfOut);


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

SynthDef(\phaseVocoderOCEAN, {


	arg outBus, inBus, pitchShiftAmount = 2,
	fftSize = 8192, winLen = 4096, overlap=0.25, inWinType = 0, outWinType = 0;

	var in, chain, multiplier, newBin;

	multiplier = (2*pi * p) / (overlap*fftSize);





	in = In.ar(inBus, 1);


	chain = FFT(LocalBuf(fftSize), in, overlap, inWinType, 1, winLen);

	//Shift bins
	chain = PV_BinShift(chain, pitchShiftAmount, 0.5);


	//Adjust phase


	chain = chain.pvcollect(1024,{ arg magnitude, phase, bin, index;
		p = p + 1;

		[magnitude, phase + (pitchShiftAmount*bin-bin+0.5)*multiplier]
	});



	chain = IFFT(chain);
	//chain = LPF.ar(chain, 8000);
	Out.ar(outBus, chain);



}).add;






SynthDef(\readInputSignal, {

	arg outBus = 0;

	//Adjust argument to your input port
	Out.ar(outBus, SoundIn.ar(1));

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



var lowOctLevel, highOctLevel, dryOctLevel, btnOctLow, btnOctHigh, btnDry, setPoly;
var column1, column2, column3;
var t1, t2, t3, t4, t5, t6;
var amp;

var lowOctEffect, highOctEffect, dryOctEffect;
var dispertionLowOct, dispertionLow2Oct, dispertionHiOct, dispertionHi2Oct;
var w = Window.new("GUI Introduction", Rect(200,200,1000,300));




//Bus and SynthDef setup

octUp1PolySD = Synth(\phaseVocoderOCEAN, [\inBus, octaveUp1BusPolyphonic]);
octDown1PolySD = Synth(\phaseVocoderOCEAN, [\inBus, octaveDown1BusPolyphonic,
	\pitchShiftAmount, 0.5]);
octDown1MonoSD = Synth(\octaveDown1, [\inBus, octaveDown1BusMonophonic]);
octUp1MonoSD = Synth(\octaveUp1, [\inBus, octaveUp1BusMonophonic]);
z = Synth(\octaverMain, [\inBus, inputBus, \octaveUp1Bus, octaveUp1BusMonophonic,
	\octaveDown1Bus, octaveDown1BusMonophonic,
]);


h = Synth(\readInputSignal, [\outBus, inputBus]);





//GUI setup


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
	["OFF", Color.white, Color.red(val:0.6, alpha:0.5)],
	["ON", Color.black, Color.green(val:0.7, alpha:0.5)]
        ]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==0,
		{
			lowOctLevel.background_(Color.red(val:0.8, alpha:0.2));
			lowOctEffect.background_(Color.yellow(val:0.65, alpha:0.01));
			//Turn low octave level to zero
			z.set(\octaveDown1KnobLevel, 0);
		},
		{
			lowOctLevel.background_(Color.red(val:0.8, alpha:0.5));
			lowOctEffect.background_(Color.yellow(val:0.95, alpha:0.01));
			//Reset low octave level to level given by the knob
			z.set(\octaveDown1KnobLevel, lowOctLevel.value);
		}
	);

});

btnOctLow.value=1;




btnDry = Button(parent:w, bounds:Rect(440, 30, 30, 30)).states_([
	["OFF", Color.white, Color.red(val:0.6, alpha:0.5)],
	["ON", Color.black, Color.green(val:0.7, alpha:0.5)]
        ]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==0,
		{
			dryOctLevel.background_(Color.red(val:0.8, alpha:0.2));
			dryOctEffect.background_(Color.yellow(val:0.65, alpha:0.01));
			//Turn low octave level to zero
			z.set(\dryKnobLevel, 0);
		},
		{
			dryOctLevel.background_(Color.red(val:0.8, alpha:0.5));
			dryOctEffect.background_(Color.yellow(val:0.95, alpha:0.01));
			//Reset low octave level to level given by the knob
			z.set(\dryKnobLevel, dryOctLevel.value);
		}
	);

});
btnDry.value=1;



btnOctHigh = Button(parent:w, bounds:Rect(620, 30, 30, 30)).states_([
	["OFF", Color.white, Color.red(val:0.6, alpha:0.5)],
	["ON", Color.black, Color.green(val:0.7, alpha:0.5)]
        ]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==0,
		{
			highOctLevel.background_(Color.red(val:0.8, alpha:0.2));
			highOctEffect.background_(Color.yellow(val:0.65, alpha:0.01));
			//Turn low octave level to zero
			z.set(\octaveUp1KnobLevel, 0);
		},
		{
			highOctLevel.background_(Color.red(val:0.8, alpha:0.5));
			highOctEffect.background_(Color.yellow(val:0.95, alpha:0.01));
			//Reset low octave level to level given by the knob
			z.set(\octaveUp1KnobLevel, highOctLevel.value);
		}
	);

});

btnOctHigh.value = 1;



//volume sliders, low, high, original
//Sliders are from 0 to 1 by default

lowOctLevel = Knob.new(w,Rect(220,90,110,30)).background_(Color.red(val:0.8, alpha:0.5));
t1 = CompositeView.new(w,Rect(265,60,200,30));
StaticText.new(t1,Rect(0,0,150,30)).string_("vol");
lowOctLevel.action_({
	arg knob;
  z.set(\octaveDown1KnobLevel, knob.value);
	knob.value.postln;

});


dryOctLevel = Knob.new(w,Rect(400,90,110,30)).background_(Color.red(val:0.8, alpha:0.5));
t2 = CompositeView.new(w,Rect(445,60,200,30));
StaticText.new(t2,Rect(0,0,150,30)).string_("vol");
dryOctLevel.action({
	arg knob;
	z.set(\dryKnobLevel, knob.value);
});
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
	arg knob;
	z.set(\octaveUp1KnobLevel, knob.value);

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
	["monophonic", Color.black, Color.green(val:0.7, alpha:0.5)],
    ["polyphonic", Color.white, Color.red(val:0.6, alpha:0.5)]
]).action_({ arg butt;
            butt.value.postln;
	if (butt.value==1,
		{
			w.background_(Color.grey(grey:0.5, alpha:0.92));
			//Switch busses to polyphonic
			z.set(\octaveUp1Bus, octaveUp1BusPolyphonic);
			z.set(\octaveDown1Bus, octaveDown1BusPolyphonic);

		},
		{
			w.background_(Color.grey(grey:0.95, alpha:0.5));
			//Switch busses to monophonic
			z.set(\octaveUp1Bus, octaveUp1BusMonophonic);
			z.set(\octaveDown1Bus, octaveDown1BusMonophonic);
		}
	)
});




)






