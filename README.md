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

## 1. Description
Implementation of an octaver effect in SuperCollider for the 2022 CMLaS course homework #1. This effect allows the user to mix a live input signal with a synthetised signal whose musical tone is one octave above or below the original one. Additionally, a chorus effect can be added after the octaver stage.

#
#
## 2. GUI
### Control knobs
*  Dry/Wet Knob: controls amount of the original dry input signal that is let through unaltered.
*  Octave Up/Down Knob: controls how much of the ”wet” signal will be composed of the lower or the upper octave signal. Turning the knob completely to the left (minimum value) will output only the lower octave, while turning it completely to the right (maximum value) will output only the upper one. 
*   Amplitude knob: controls the total output volume
*   LPF Cutoff: controls the frequency cutoff of a low-pass filter which is applied after the octaver stage when the monophonic mode is engaged
*   Chorus knobs: control the parameters of the chorus applied to the signal
### Polyphonic - Monophonic button
Allows the user select which algorithm will be employed to synthesise the octave-modified signals. When the switch is off the effect acts in Monophonic mode, delivering a more ”vintage” sound which mimics the one produced by classic analogue octaver pedals. The Polyphonic mode offers a less interesting and more ”pitch- shifting”-like digital sound, though it is able to handle chords.
#
#
## 3. Implementation
### Main SynthDefs
* OctaverMain: distribut
es the signal coming from its input bus across all the audio busses which will be read by the effect SynthDefs;
* OctaveUpMonophonic:  used to receive the input signal and synthetise a signal an octave above (when monophonic mode is engaged);
* OctaveDownMonophonic: used to receive the input signal and synthetise a signal an octave below (when monophonic mode is engaged);
* OctaveUpPolyphonic: used to receive the input signal and synthetise a signal an octave above (when polyphonic mode is engaged);
* OctaveDownPolyphonic: used to receive the input signal and synthetise a signal an octave below (when polyphonic mode is engaged);
* Chorus:   applied  to  the  signal  after  the  octaver  stage
#
#
