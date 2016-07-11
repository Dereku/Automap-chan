import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JOptionPane;

public class MidiToOsuConverter implements ActionListener, Runnable {
	// User Input
	private String filename;
	private boolean extractNotes;
	private int keyCount;
	private int LN_Cutoff=999999;
	private int volume=100;
	private boolean customHS = false;
	private String convert = System.getProperty("user.dir") + "\\convert.csv";
	private int DifficultyOfDensity = 7;
	private String artist = "Unknown";
	private int MAX_CHORD = 3;
	private int sampleRate = 16000;
	private int bitDepth = 16;
	private int channelMode = 1;
	private boolean customTiming = false;
	private boolean mergeHS;

	// Constants
	private int bpm;
	private final int NOTE_ON = 0x90;
	private final int NOTE_OFF = 0x80;
	private final String[] NOTE_NAMES = { "C", "C#", "D", "D#", "E", "F", "F#",
			"G", "G#", "A", "A#", "B" };
	private String hitsoundPath;
	private String outputPath;
	private Sequencer sequencer = null;
	private int HitSoundSize = 0;
	private int outputSize = 1;
	private final String version;
	private int [] options;
	private WindowProgress progressWindow = null;
	private String midiPath;
	TreeMap<String, String> hsHM = new TreeMap<String, String>();
	// tempo
	ArrayList<Long> absTimeline = new ArrayList<Long>();
	ArrayList<Long> tickTimeline = new ArrayList<Long>();
	ArrayList<Long> tempoArray = new ArrayList<Long>();
	//instrument
	int[] instruments = new int[16];
	private int currentSize = 0;
	
	// Constructor
	public MidiToOsuConverter(Sequencer seq, String name,
			Boolean extractNotes, int keys, int maxChord, int OD, int[] trackOptions,boolean mergeHitSound) {
		readFromProperty(System.getProperty("user.dir"));
		if (seq == null){
			throw new IllegalArgumentException("Null Sequencer");
		}
		sequencer = seq;
		this.extractNotes = extractNotes;
		keyCount = keys;
		MAX_CHORD = maxChord;
		DifficultyOfDensity = OD;
		filename = name;
		options = trackOptions;
		version = "KS" + DifficultyOfDensity + "-" + keyCount+"K";
		hitsoundPath = midiPath + "\\"
				+ filename.substring(0, filename.length() - 4) + "_hitsounds\\";
		outputPath = midiPath + "\\" + filename.substring(0, filename.length() - 4)
				+ "_outputs\\";
		mergeHS = mergeHitSound;
	}

	private void readFromProperty(String path) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(path + "\\config.properties");
			prop.load(input);
			midiPath = prop.getProperty("midiPath");
			input.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	public void setConvert(String input) {
		convert = input;
	}

	public void run() {
		if (extractNotes && customHS == false) {
			Utils.createFolder(hitsoundPath);
		}
		Utils.createFolder(outputPath);
		if (customHS) {
			loadHSConvert(convert);
		}
		try {
			if (customTiming){
				long[] timings = {0L,33L,451L,37L};
				int[] bpms = {120,150,115,125};
				String file = System.getProperty("user.dir")  + "\\customBPM.mid";
				sequencer = MidiUtils.emptyTempos(sequencer);
				MidiUtils.setTempos(sequencer, timings, bpms);
				//MidiUtils.keepOnlyTrack(sequencer, 4);
				MidiUtils.saveMidi(sequencer, file);
			}
			loadTimeline(sequencer);
			ArrayList<NoteArray> info = getMidiInfo(sequencer, options);
			toOsuBeatmap(info);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// display finished
		JOptionPane.showMessageDialog(null, "Finished!");
	}

	/**
	 * Output a note as .wav file
	 * 
	 * @param note
	 *            Note to output
	 * @param filename
	 *            complete path
	 * @throws MidiUnavailableException
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	private void extractNote(Note note, String filename)
			throws MidiUnavailableException, InvalidMidiDataException,
			IOException {

		try {
			int channel = note.getChannel();
			Sequencer sequencer = MidiSystem.getSequencer();
			int resolution = this.sequencer.getSequence().getResolution();
			float divisionType = this.sequencer.getSequence().getDivisionType();
			Sequence seq = new Sequence(divisionType, resolution);
			sequencer.setSequence(seq);
			Track track = seq.createTrack();

			// set instrument
			ShortMessage instrumentChange = new ShortMessage();
			int instrument = note.getInstrument();
			instrumentChange.setMessage(ShortMessage.PROGRAM_CHANGE, channel,
					instrument, 0);
			track.add(new MidiEvent(instrumentChange, 0));
			
			// set channel volume
			int channelVolume = note.getChannelVolume();
			// System.out.println(channelVolume);
			ShortMessage channelVolumeChange = new ShortMessage();
			channelVolumeChange.setMessage(ShortMessage.CONTROL_CHANGE,
					channel, 7, channelVolume);
			track.add(new MidiEvent(channelVolumeChange, 0));
			// add note
			int key = note.getKey();
			int velocity = note.getVelocity();
			int startT = 0;
			int endT = note.getDuration();
			// set tempo, us per tick
			long tempo = Utils.tickToMidiTempo(note.getBPM());
			MetaMessage mm = new MetaMessage();
			byte[] data = MidiUtils.tempoToDataBytes(tempo);
			mm.setMessage(81, data, 3);
			track.add(new MidiEvent(mm, 0));
			// start
			ShortMessage msg = new ShortMessage();
			msg.setMessage(NOTE_ON + channel, key, velocity);
			track.add(new MidiEvent(msg, startT));
			// end
			ShortMessage msg2 = new ShortMessage();
			msg2.setMessage(NOTE_OFF + channel, key, 0);
			track.add(new MidiEvent(msg2, endT));
			// output wav
			int SR = sampleRate;
			double duration = Utils.tickToMilliSec(endT,
					resolution, note.getBPM()) / 1000.0;
			if (note.getKey() > 69) {
				SR = 16000;
			}
			duration = duration + 10;
			MidiToWavRenderer w = new MidiToWavRenderer(SR, bitDepth,
					channelMode);
			w.createWavFile(seq, key, new File(filename), duration);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
	
	

	// Output an array of notes to .wav file
	private void extractAllNotes(NoteArray notes)
			throws MidiUnavailableException, InvalidMidiDataException,
			IOException {
		progressWindow.display("Number of hit sounds to extract : " + notes.getSize());
		Iterator<Note> ite = notes.iterator();
		while (ite.hasNext()) {
			Note n = (Note) ite.next();
			String filename = hitsoundPath + n.getHitSound();
			n.setVelocity(100);
			extractNote(n, filename);
			currentSize++;
			progressWindow.updateProgress(currentSize);
		}
		progressWindow.display("Finished extracting all hit sounds");
	}

	private void loadHSConvert(String csvFile) {
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";

		try {

			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {

				// use comma as separator
				String[] note = line.split(cvsSplitBy);
				hsHM.put(note[0], note[1]);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private void loadTimeline(Sequencer sequencer) throws Exception{
		if (tempoArray.size()>=1){
			return;
		}
		if (sequencer.getSequence().getDivisionType() != Sequence.PPQ) {
			System.out.println("Division Type = " + sequencer.getSequence().getDivisionType());
			throw new Exception("Unsupported for now");
		}
		// resolution is ticks per beat, 1 beat is 1 quarter note
		int res = sequencer.getSequence().getResolution();

		for (int i = 0; i < 16; i++) {
			instruments[i] = 0;
		}
		// default tempo is 120 bpm = 2 beat per sec = 1 beat is 500ms =
		// 500000us per beat
		long tempo = 500000;
		// read all bpm changes and store it into absolute and tick time lines
		for (Track track : sequencer.getSequence().getTracks()) {
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				long t = event.getTick();
				MidiMessage message = event.getMessage();

				if (message instanceof MetaMessage) {
					MetaMessage mm = (MetaMessage) message;
					if (mm.getType() == 81) {
						// tempo message
						byte[] data = mm.getData();
						// tempo is duration of quarter note in micro sec
						tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8
								| (data[2] & 0xff);
						//System.out.println("tempo event at t = " + t + " is " + tempo);
						
						if (tempoArray.size() == 0) {
							tempoArray.add(tempo);
							tickTimeline.add(t);
							absTimeline.add(Utils.tickToMilliSec(t, res, 120));
						} else {
							int index = tickTimeline.size() - 1;
							long tickDuration = t - tickTimeline.get(index);
							long absTime = absTimeline.get(index)
									+ Utils.tickToMilliSec(tickDuration,res,
											tempoArray.get(index));
							if (!absTimeline.contains(absTime)) {
								// store new tempo
								tempoArray.add(tempo);
								// store time into tick time line
								tickTimeline.add(t);
								// store time into absolute time line
								absTimeline.add(absTime);
							} else  {
								tempoArray.remove(tempoArray.size() - 1);
								tempoArray.add(tempo);
							}
						}
							

					}

				} else if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
					int status = sm.getStatus();
					if (status >= 0xC0 && status <= 0xCF) {
						// Program change (instrument)
						instruments[sm.getChannel()] = sm.getData1();
					}
				}

			}// end of track

		}
		if (tempoArray.size() == 0) {
			tempoArray.add(500000L);
			tickTimeline.add(new Long(0));
			absTimeline.add(new Long(0));
		}

		System.out.println(tempoArray);
	}
	
	/**
	 * 
	 * @param sequencer
	 * @return an ArrayList containing first the total notes, and then all the notes to extract as hitsounds
	 * @throws Exception
	 */
	public ArrayList<NoteArray> getMidiInfo(Sequencer sequencer, int[] options) throws Exception{
		ArrayList<NoteArray> output = new ArrayList<>();
		long time = 0;
		int resolution = this.sequencer.getSequence().getResolution();
		// list of unique notes for all tracks
		NoteArray allTrackUniqueNotes = new NoteArray();
		// List of all notes
		NoteArray totalNotes = new NoteArray();
		NoteArray BGNotes = new NoteArray(); 
		bpm = (int) sequencer.getTempoInBPM();
		int trackID = 0;
		for (Track track : sequencer.getSequence().getTracks()) { // for each track
			int toBG = options[trackID];
			if (toBG >=0){
				NoteArray notes = new NoteArray(); // temporary array of
				int instrument = -1;
				int channelVolume = 100;
				for (int i = 0; i < track.size(); i++) {
					String line = "";
					MidiEvent event = track.get(i);
					time = event.getTick();
					line = line + "@" + time + " ";

					MidiMessage message = event.getMessage();
					if (message instanceof ShortMessage) {
						ShortMessage sm = (ShortMessage) message;
						bpm = Utils.getBpm(tickTimeline, tempoArray, time);
						int status = sm.getStatus();
						if (status >= 0xB0 && status <= 0xBF) {
							// volume change
							if (sm.getData1() == 7) {
								channelVolume = sm.getData2();

							}

						}
						// NOTE ON
						if (sm.getCommand() >= 0x90 && sm.getCommand() <= 0x9F) {
							int channel = sm.getChannel();
							instrument = instruments[channel];
							int key = sm.getData1();
							int octave = (key / 12) - 2;
							int note = key % 12;
							String noteName = NOTE_NAMES[note];
							int velocity = sm.getData2();
							

							Note n = null;
							long absT = -1;
							if (velocity > 0) {
								// Note ON
								// resolution = tick per beat
								// bpm = beat per min
								int index = Utils.getIndexFromTimeline(time,
										tickTimeline);
								long tickDuration = time - tickTimeline.get(index);
								long startT = absTimeline.get(index);
								absT = startT
										+ Utils.tickToMilliSec(tickDuration, resolution,
												tempoArray.get(index));
								if (absT < 0) {
									System.out.println("tick time = " + time);
									System.out.println("absolute time = " + absT);
									System.out.println("tickDuration duration = "
											+ tickDuration);
									throw new IllegalArgumentException();
								}

								if (octave < 0) {
									octave = Math.abs(octave);
									n = new Note(noteName + "_" + octave, velocity,
											time, key, bpm, absT, instrument,
											channelVolume);
								} else {
									n = new Note(noteName + octave, velocity, time,
											key, bpm, absT, instrument,
											channelVolume);
								}
								n.setChannel(channel);
								notes.add(n);
								line = line + "Note on, " + noteName + octave
										+ " velocity: " + velocity + "\n";
							} else if (time>0){
								// Note Off
								treatNoteOFF(notes, totalNotes, BGNotes,
										allTrackUniqueNotes, octave, noteName,
										velocity, key, time, resolution, toBG);
								line = line + "Note off, " + noteName + octave
										+ " velocity: " + velocity + "\n";
							}
						} else if (sm.getCommand() >= 0x80
								&& sm.getCommand() <= 0x8F && time>0) {
							// Note Off
							int key = sm.getData1();
							int octave = (key / 12) - 2;
							int note = key % 12;
							String noteName = NOTE_NAMES[note];
							int velocity = sm.getData2();

							treatNoteOFF(notes, totalNotes, BGNotes,
									allTrackUniqueNotes, octave, noteName,
									velocity, key, time, resolution,toBG);
							line = line + "Note off, " + noteName + octave
									+ " velocity: " + velocity + "\n";
						}
					}

				}// end of 1 track
				
			}// end of if (toBG>= 0)
			trackID++;
		}// end of tracks
		
		
		// osu hit objects
		output.add(totalNotes);
		// hit sounds
		output.add(allTrackUniqueNotes);	
		// BG Notes
		output.add(BGNotes);
		return output;
	}
	
	
	public void toOsuBeatmap(ArrayList<NoteArray> input) throws MidiUnavailableException, InvalidMidiDataException, IOException{
		NoteArray totalNotes = input.get(0);
		if (totalNotes.getSize()==0){
			System.out.println("No notes to convert!");
		}
		NoteArray allTrackUniqueNotes = input.get(1);
		NoteArray BGNotes = input.get(2);
		//Timing Section
		String timingSections = "";
		for (int i = 0; i < absTimeline.size(); i++) {
			Timing tp = new Timing(absTimeline.get(i), Utils.tempoToMilliSec(tempoArray.get(i)));
			timingSections += tp.toOsuTimingPoint();
		}

		if (extractNotes && customHS == false) {
			if (!Utils.isFolderEmpty(hitsoundPath)) {
				int reply = JOptionPane
						.showConfirmDialog(
								null,
								"Do you wish to replace existing files within hit sound folder?",
								"Midi to Osu Converter",
								JOptionPane.YES_NO_OPTION);
				if (reply == JOptionPane.YES_OPTION) {
					Utils.emptyFolder(hitsoundPath);
				} else {
					System.exit(0);
				}
			}
			HitSoundSize = allTrackUniqueNotes.getSize();
		} else{
			HitSoundSize = 0;
		}
		progressWindow = new WindowProgress();
		progressWindow.setProgressMax(this.HitSoundSize + this.outputSize);
		progressWindow.display("total number of notes = " + totalNotes.getSize());
		ArrayList<NoteArray> list = totalNotes.sortNotesByTime();
		ChartingAlgorithm ChartConv = new ChartingAlgorithm();
		ChartConv.setColumns(list, keyCount, MAX_CHORD, DifficultyOfDensity);
		if ( BGNotes.getBGSize()>0 && totalNotes.getSize()>0){
			ArrayList<NoteArray> bgList = BGNotes.sortBGNotesByTime();
			list = addBGNotes(list,bgList);
		}
		String osuOutput = "";
		String sampleOutput = "//Storyboard Sound Samples\n"; // all the bg

		
		Iterator<NoteArray> ite = list.iterator();
		while (ite.hasNext()) {
			NoteArray chord = ite.next();
			chord.sortByPitch();
			sampleOutput += chord.toBackgroundSample(volume);
			osuOutput += chord.toHitObjects(keyCount,
					sequencer.getSequence().getResolution(), LN_Cutoff, volume);
		}
		OsuBeatmap beatmap = new OsuBeatmap(
				Utils.getFilenameWithoutExtensionFromPath(filename),
				sampleOutput, osuOutput);
		beatmap.setTimingPoints(timingSections);
		beatmap.setArtist(artist);
		beatmap.setVersion(version);
		beatmap.setKeyCount(keyCount);
		Utils.writeToFile(progressWindow,
				outputPath + artist + " - "
						+ Utils.getFilenameWithoutExtensionFromPath(filename)
						+ " (Automap-chan) [" + version + "].osu",
				beatmap.toString());
		progressWindow.display("finished outputing osu hit objects!");
		currentSize++;
		progressWindow.updateProgress(currentSize);
		if (extractNotes && customHS == false) {
			extractAllNotes(allTrackUniqueNotes);
		}

	}

    
	@SuppressWarnings("unchecked")
	private ArrayList<NoteArray> addBGNotes(ArrayList<NoteArray> listOfChords, ArrayList<NoteArray> BGNotes){
		ArrayList<NoteArray> temp = (ArrayList<NoteArray>) BGNotes.clone();
		ArrayList<NoteArray> output = (ArrayList<NoteArray>) listOfChords.clone();
		while (temp.size()!=0){
			NoteArray aBGChord = temp.get(0);
			long bgTime = aBGChord.getBGNoteFromIndex(0).getAbs();
			for (int i = 0; i<listOfChords.size();i++){
				NoteArray aChord = listOfChords.get(i);
				long time = aChord.getNoteFromIndex(0).getAbs();
				if (bgTime==time){
					int index = output.indexOf(aChord);
					aChord.addAllBGNotes(aBGChord);
					output.set(index, aChord);
					aBGChord = null;
					break;
				} else if (bgTime<time){
					int index = output.indexOf(aChord);
					output.add(index, aBGChord);
					aBGChord = null;
					break;
				}
			}
			if (aBGChord != null && aBGChord.getBGSize() != 0){
				output.add(aBGChord);
			}
			temp.remove(0);
		}
		return output;
	}
	
	
	private void treatNoteOFF(NoteArray notes, NoteArray totalNotes,
			NoteArray BGNotes, NoteArray allTrackUniqueNotes, int octave,
			String noteName, int velocity, int key, long time, int resolution, int toBG) {
		Note n = null;
		if (octave < 0) {
			octave = Math.abs(octave);
			n = new Note(noteName + "_" + octave, velocity, time, key, bpm);
		} else {
			n = new Note(noteName + octave, velocity, time, key, bpm);
		}
		if (notes.contains(n)) {
			Note previousNote = notes.getNote(n);
			n.setBPM(previousNote.getBPM());
			n.setChannel(previousNote.getChannel());
			n.setInstrument(previousNote.getInstrument());
			n.setChannelVolume(previousNote.getChannelVolume());
			long startTime = previousNote.getTime();
			long endTime = n.getTime();
			int duration = (int) (endTime - startTime);
			n.setLNduration(duration);
			if (duration == 0) {
				System.out.println("duration is 0 at time " + time);
				throw new IllegalArgumentException(n.toString());
			} else if (duration < resolution && mergeHS) {
				duration = resolution;
			} else if (duration % resolution != 0 && mergeHS) {
				int beat = duration / resolution;
				duration = beat * resolution;
			}
			n.setDuration(duration);
			n.setAbs(previousNote.getAbs());
			n.setVelocity(previousNote);
			n.setTime(previousNote.getTime());
			notes.remove(n);
			n.setCustomHS(customHS);
			if (customHS) {
				n.setName(hsHM.get(n.getName()));
			}
			if (toBG==1){
				BGNotes.addBGNote(n);
			} else if (toBG == 0) {
				totalNotes.add(n);
			}
			
			if (!allTrackUniqueNotes.contains(n, n.getDuration(),n.getBPM())) {
				allTrackUniqueNotes.add(n);
			}

		}

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// signal the worker thread to get crackin
        synchronized(this){notifyAll();}
		
	}

	

}