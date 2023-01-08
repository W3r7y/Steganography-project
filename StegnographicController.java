import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

public class StegnographicController {

	@FXML
	private Label fileNameLabel;

	@FXML
	private TextArea txtArea;

	@FXML
	private ImageView imgView1;

	@FXML
	private RadioButton hideBtn;

	@FXML
	private ToggleGroup g1;

	@FXML
	private RadioButton readBtn;

	@FXML
	private RadioButton withEnc;

	@FXML
	private ToggleGroup g2;

	@FXML
	private RadioButton withoutCrt;

	private File file;	//file that we working on
	CryptoServise cryptoServise = new CryptoServise();	//Used for encryption and decryption (implments AES)


	// The function reacts on click on "choose file" button, sets file, gets the image and shows the image on the screen;
	@FXML
	void chooseFilePressed(ActionEvent event) {
		FileChooser fc = new FileChooser();
		fc.setTitle("Select file");
		fc.setInitialDirectory(new File("."));
		file = fc.showOpenDialog(null);
		BufferedImage originalImage;
		if(file != null) {	//if no file selected than nothing happens.
			try {
				originalImage = ImageIO.read(file);
				//Set image is not accepting buffered image so to visualise i use writable image.
				WritableImage image = SwingFXUtils.toFXImage(originalImage, null);
				imgView1.setImage(image);
				fileNameLabel.setText("Working on: "+ file.getName());
			} catch (IOException e) {
				System.out.print("Something went wrong when you choosed the image");
				e.printStackTrace();
			}
		}
	}

	// The function reacts on click on "decrypt message" button, gets the text that shown on text area,
	// asks for password (key) and tries to decrypt the text.
	@FXML
	void decryptPressed(ActionEvent event) {
		String extractResult = txtArea.getText();
		if(extractResult != "") {	//if there is some text to decrypt
			TextInputDialog dialog2 = new TextInputDialog();
			dialog2.setTitle("Varification");
			dialog2.setHeaderText("What is the password for decryption?");
			dialog2.setContentText("Password:");      
			dialog2.showAndWait();
			String providedPassword = dialog2.getResult();	//Password that user provides
			String decryptedResult = cryptoServise.decrypt(extractResult, providedPassword);
			txtArea.setText(decryptedResult);
		}
	}

	//The function ets the password for encryption
	private String setPassword() {
		String password;
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Set your password");
		dialog.setHeaderText("Set a password to encrypt the massage that you want to hide");
		dialog.setContentText("Password:");      
		dialog.showAndWait();
		password = dialog.getResult();
		CryptoServise.setKey(password);	//stores the pass in the crypto servise
		return password;
	}

	//The function that restarts the settings of the user interface 
	@FXML
	void cleanPressed(ActionEvent event) {
		txtArea.setText("");
		imgView1.setImage(null);
		file = null;		//not working on any file
		fileNameLabel.setText("File name");

		//radio buttons off
		hideBtn.setSelected(false);
		readBtn.setSelected(false);
		withEnc.setSelected(false);
		withoutCrt.setSelected(false);
	}

	//The function that handles the click on "start" button, get all the setting information that user choosed, 
	//hides or reads the message from the image that the user choosed.
	@FXML
	void startPressed(ActionEvent event) throws IOException {
		String str;

		//THIS PART IS TO CODE MESSAGE INSIDE THE PICTURE		
		if(g1.getSelectedToggle() == hideBtn) {

			//Get Text from text Field
			str = txtArea.getText();
			txtArea.setText("");	//clean 
			BufferedImage originalImage;
			originalImage = ImageIO.read(file);

			//user choosed to encrypt his message
			if(g2.getSelectedToggle() == withEnc) {
				String password;
				password = setPassword();	//gets password from the user
				CryptoServise.setKey(password);	//sets the password for cryptoServise
				String ecryptedStr = cryptoServise.encrypt(str, password);	// encrypt the text (string)

				//Popup with information for the user
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Important message");
				alert.setHeaderText("Important message");
				alert.setContentText("Your secret lengh number is "+ecryptedStr.length()+", please remember this one, you will need it to decrypt the message");
				alert.showAndWait();


				hideText(originalImage, ecryptedStr);	//hide encrypted text inside the image
			}

			// user choosed to not encrypt the message
			else if(g2.getSelectedToggle() == withoutCrt) {
				//Popup with information for the user
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Important message");
				alert.setHeaderText("Important message");
				alert.setContentText("Your message lengh is " +str.length() +" bytes, remember this number to extract data back from the picture.");
				alert.showAndWait();

				hideText(originalImage, str);	//hide NOT encrypted text inside the image
			}		
		}


		//THIS PART IS TO GET BACK THE MESSAGE OUT OF THE PICTURE				
		if(g1.getSelectedToggle() == readBtn) {
			txtArea.setText("");	//clean the board

			//Asks for information about the length of the data that user want to extract from the image			
			TextInputDialog dialog1 = new TextInputDialog();
			dialog1.setTitle("Extraction Length");
			dialog1.setHeaderText("How many bytes to extract");
			dialog1.setContentText("Number of bytes:");
			dialog1.showAndWait();
			str = dialog1.getResult();

			try {
				int num = Integer.parseInt(str);
				extractText(ImageIO.read(file),num);	//extract the text from the file(image) that we working on
			}catch (Exception e) {		//if the input is not a number
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Invalid input");
				alert.setHeaderText("Invalid input");
				alert.setContentText("Something went wrong, invalid input provided.");
				alert.showAndWait();
			}
		}
	}


	//The function gets an image and data that we want to hide inside the image,
	//runs on the pixels (RGBs) of the picture and sets the last bit of every pixel as data bit (the data we hide)
	//first bit of data hided in the first pixels last bit, and so on.

	private void hideText(BufferedImage image, String data) {
		int x = 0;				// starting index of pixel (x,y)
		int y = 0;				// starting index of pixel (x,y)
		int byteOfData;				// integer representation of ASCII number of a char
		int bitMask = 0x00000001;	// mask to get the last digit of char

		//for every char in the data text
		for(int i=0; i < data.length(); i++) {			
			byteOfData = (int) data.charAt(i);		// bit gets the ASCII number of char (byte)

			//for every bit in the byteOfData 
			for(int j = 0; j < 8; j++) {
				int flag = byteOfData & bitMask;	// gets the only bit
				if(flag == 1) {		//if databit is 1
					if(x < image.getWidth()) {	//check if we didnt pass the last pixel in row
						image.setRGB(x, y, image.getRGB(x, y) | 0x00000001); 	// set 1 into a pixel's last digit
						x++;
					}
					else {	//passed the last pixel in row
						x = 0;
						y++;
						image.setRGB(x, y, image.getRGB(x, y) | 0x00000001); 	// set 1 into a pixel's last digit
					}
				} 
				else {	//if databit is 0
					if(x < image.getWidth()) {
						image.setRGB(x, y, image.getRGB(x, y) & 0xFFFFFFFE);	// set 0 into a pixel's last digit
						x++;
					}
					else {
						x = 0;
						y++;
						image.setRGB(x, y, image.getRGB(x, y) & 0xFFFFFFFE);	// set 0 into a pixel's last digit
					}
				}
				byteOfData = byteOfData >> 1;				// next digit (move all digits right by 1)
			}			
		}

		// save the image which contains the secret information to another image file
		try {
			FileChooser fc = new FileChooser();
			fc.setTitle("Save file");
			fc.setInitialDirectory(new File("."));
			fc.setInitialFileName("example1.jpg");
			File outputfile = fc.showSaveDialog(null);	
			if(outputfile != null) {
				ImageIO.write(image, "png", outputfile);	//png is "loseless format" for buffer images when every pixel metters.
				//jpeg does not suit for us.
				//more information provided here 
				//https://www.javamex.com/tutorials/graphics/bufferedimage_save_png_jpeg.shtml
			}

		} catch (IOException e) {
			System.out.println("Somethig went wrond during file saving");
		}		
	}



	//The function gets an image and the length of data that we want to extract from the image,
	//runs on each and every pixel to get the exact lenght of bytes of information,
	//the function sets the extracted data on the text area in the user interface.

	private void extractText(BufferedImage image, int length) {
		int x = 0;				// starting index of pixel (x,y)
		int y = 0;				// starting index of pixel (x,y)
		int bitMask = 0x00000001;	// mask to get the last digit of char
		int flag;	//used to flag if last digit of extracted data is 1 or zero
		char[] c = new char[length] ;	// array of char to store the infomation that we extract

		//Until we get the exact number of bytes of data(length)
		for(int i = 0; i < length; i++) {	
			int byteOfData = 0;

			// for every digit in char(byte)
			for(int j = 0; j < 8; j++) {				
				if(x < image.getWidth()) {	//check that we are not out of range in the row
					flag = image.getRGB(x, y) & bitMask;	// get the last digit of the pixel
					x++;	//next pixel in the row
				}
				else {
					x = 0;	//first pixel in the row
					y++;	//next column
					flag = image.getRGB(x, y) & bitMask;	// get the last digit of the pixel
				}

				// sets the extracted digit in the byte of data
				if(flag == 1) {					
					byteOfData = byteOfData >> 1;	
				byteOfData = byteOfData | 0x80;	//8th bit in HEX
				} 
				else {					
					byteOfData = byteOfData >> 1;	//move all bit to the right
				}				
			}
			//Extracted byte of data represented in ASCII format at the text area 
			c[i] = (char) byteOfData;	// cast from int to char(byte) to represenct the ASCII
			txtArea.setText(txtArea.getText() + c[i]);
		}
	}
}