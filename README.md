# WikiSpiv-BookMaker
Program for creating printed songbooks based on the wikispiv db

## Using the Program
* Download the latest release from the [https://github.com/daniel-centore/WikiSpiv-BookMaker/releases](releases) page
* Run the file (you may need to install [https://www.java.com/en/download/](Java) first)
* Create a new, empty folder for the spivanyk you will be creating (e.g. "Tabir Spivanyk 2024")
* Inside that folder, create folders called "Fonts" and "Images" (capitalization matters!)
* Inside the "Fonts" folder, add any .ttf files for fonts you would like to include
  * You must validate that the font actually includes Cyrillic letters first (including Ukrainian-specific letters like "Ї")
  * https://www.ukrfonts.com/ is a good place to find these
* 

## Making Lulu-ready book
* Export PDF from WikiSpiv Bookmaker
* Make any manual edits to the PDF (e.g. adding intro pages)
* Open the PDF in Adobe Acrobat and export as Postscript
* Open Acrobat Distiller
  * Drag the regular Lulu jobsettings in ( [http://connect.lulu.com/en/discussion/33681/pdf-creation-settings-how-can-i-be-sure-my-pdf-will-print-correctly](https://help.lulu.com/en/support/solutions/articles/64000255519-pdf-creation-settings) )
  * Drag in the postscript file
* Conversion done!
  
