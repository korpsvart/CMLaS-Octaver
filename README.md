# HOMEWORK 1 - OCTAVER
# 
## GROUP DELAYLAMA
* Ahmed Said
* Riccardo Di Bella
* Juan Braun 
* Sofia Parrinelli
* Lea Bian
# 
# 

## 1. Probem description
The aim of the project is to develop an octaver using the SuperCollider programming language. The octaver  mixes the input signal with a synthesised signal whose musical tone is one octave lower or higher than the original.
#
#
## 2. GUI
### Control knobs
*  Dry/Wet Knob: controls amount of the original dry input signal that is let through unaltered.
*   The Octave Up/Down Knob: controls how much of the ”wet” signal will be composed of the lower or the upper octave signal. Turning the knob completely to the left (minimum value) will output only the lower octave, while turning it completely to the right (maximum value) will output only the upper one. 
*   Amplitude knob: controls the total output volume
*   Chorus knobs: control the parameters of the chorus applied to the signal
### Polyphonic - Monophonic button
Allows the user select which algorithm will be employed to synthesise the wet signal. When the switch is off the effect acts in Monophonic mode, delivering a more vintage sound which mimics the one produced by classic analogue octaver pedals. The Polyphonic mode offers a less interesting and more ”pitch- shifting”-like digital sound, though it is able to handle chords.
#
#
## 3. Implementation
### SynthDefs
* octaverMain: distribut
es the signal coming from its input bus across all the audio busses which will be read by the effect SynthDef
* octaveUpMonophonic:  used to receive the input signal and synthetise a signal an octave above.
To double the frequency of the signal, full-wave rectification is performed. In the digital domain, this simply equates to taking the absolute value of the signal. After this, we remove the DC component introduced by the previous operation and then apply a low-pass filter to smooth out the high frequency components, introduced by the abrupt changes which appear at the end of every cycle.
* octaveDownMonophonic: in this case we perform half-wave rectification of the input signal. This half rectified signal triggers a Flip Flop that changes it output every period of the original signal, resulting in a square wave of period twice as long as the original signal. The output of Flip Flop is multiplied with the half rectified signal to obtain the lower octave signal. 
* octaveUpPolyphonic: computes the FFT of the input signal and applies the method PV_BinShift in order to shift the computed frequency bins to twice their original frequency.
* octaveDownPolyphonic: computes the FFT of the input signal and appies the method PV_BinShift in order to shift the computed frequency bins to twice their original frequency.
* chorus effect:   applied  to  the  signal  after  the  octaver  stage
#
#
## 4. Results and considerations
