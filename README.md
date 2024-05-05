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

## 2. Instructions

Open the 'Octaver.scd' file in SuperCollider, select all the code and evaluate the selection. The Octaver GUI should appear and be ready to use.

You might have to adjust some parts of the code according to which input/output ports you wish to use.
For example, you can alter the next line to match the desired input channel number (by default, the first input channel is used):
```
Out.ar(outBus, SoundIn.ar(0));
```
#
#
## 3. GUI
### Control knobs
*  Dry/Wet Knob: controls amount of the original dry input signal that is let through unaltered.
*  Octave Up/Down Knob: controls how much of the ”wet” signal will be composed of the lower or the upper octave signal. Turning the knob completely to the left (minimum value) will output only the lower octave, while turning it completely to the right (maximum value) will output only the upper one. 
*   Amplitude knob: controls the total output volume
*   LPF Cutoff: controls the frequency cutoff of a low-pass filter which is applied after the octaver stage when the monophonic mode is engaged
*   Chorus knobs: control the parameters of the chorus applied to the signal
### Polyphonic - Monophonic button
Allows the user select which algorithm will be employed to synthesise the octave-modified signals. When the switch is off the effect acts in Monophonic mode, delivering a more ”vintage” sound which mimics the one produced by classic analogue octaver pedals. The Polyphonic mode offers a less interesting and more ”pitch- shifting”-like digital sound, though it is able to handle chords.

<p align="center">
<img src="https://user-images.githubusercontent.com/93470653/161559513-d7656ba8-4b27-4925-9434-5eedb0707ae2.png" width=60% height=60% centered>
</p>

#
#

