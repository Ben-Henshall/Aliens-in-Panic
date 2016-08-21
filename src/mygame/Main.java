/*  Aliens In Panic - V1.0
 * Author:  Benjamin Henshall
 *          Ben@henshall.plus.com
 *          U3BH
 *          200965080
 * 
 * Tools used:      jMonkey Engine 3 (With Netbeans IDE)
 *                  Blender with OgreXMLconverter
 * 
 * Resources used:  jMonkey wiki/forum
 *                  http://hyperphysics.phy-astr.gsu.edu/hbase/elacol.html
 * 
 * Models are original.
 * 
 * Textures used:   Background texture for MainMenu = http://sambees.deviantart.com/art/Starry-Sky-Stock-102324102
 *                  Texture used for table model = Unknown, sorry :(
 * 
 * Audio used:      Warp 700 - Aliens in Panic (World Premiere)            
 *                  Space Dimension Controller - The Love Quadrant
 */

package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.math.*;
import com.jme3.input.controls.*;
import com.jme3.input.KeyInput;
import com.jme3.bounding.*;
import com.jme3.collision.CollisionResults;
import com.jme3.scene.Spatial;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.ui.Picture;

public class Main extends SimpleApplication {

    public static void main(String[] args) {

        Main app = new Main();
        app.start();
    }

    //List of possible Game States used in update loop
    private enum GameState {

        mainMenu, gameRunning, gamePaused
    }
    private GameState state;

    //List of possible Menu States used by ActionListener to determine which action to take
    private enum MenuState {

        onePlayer, twoPlayer, exit, easy, medium, hard
    }
    private MenuState menuState;

    //List of possible Camera States used by ActionListener to determine which camera to show
    private enum CameraState {

        topDownView, paddle1View, sideView
    }
    private CameraState camState;
    //Spatials for both paddles and puck
    protected Spatial paddle1;
    protected Spatial paddle2;
    protected Spatial puck;
    //Delcaration of pictures used for Menu Buttons
    protected Picture onePlayerButton;
    protected Picture twoPlayerButton;
    protected Picture exitButton;
    protected Picture resumePauseMenu;
    protected Picture exitPauseMenu;
    protected Picture PauseBackground;
    protected Picture easyMenu;
    protected Picture mediumMenu;
    protected Picture hardMenu;
    //Hitboxes for both paddles, the puck and both goals
    //The cylinders (paddles+puck) and spheres that never use the Y axis, making collision only in 2D
    private BoundingSphere paddle1_hitbox;
    private BoundingSphere paddle2_hitbox;
    private BoundingSphere puck_hitbox;
    private BoundingBox goalLeft;
    private BoundingBox goalRight;
    //Booleans used to check if a collision happened recently, prevents ball from constnatly colliding back and forth inside
    private boolean paddle1HasTouch = false;
    private boolean paddle2HasTouch = false;
    //Constants used to determine paddle speeds
    private final float paddle1Speed = 6.5f;
    private final float paddle2Speed = 6.5f;
    //Vectors used to determine the direction of the paddle
    private Vector3f paddle1Direction;
    private Vector3f paddle2Direction;
    //Constants for determining speed of puck, friction of puck to surface and friction of paddle to surface
    private final float friction = .9f;
    private final float puckFriction = 1.5f;
    private final float newPuckSpeed = 10f;
    //Ints for keeping track of score
    private int paddle1Score;
    private int paddle2Score;
    //Spatials used to detect goals
    private Geometry goalLeftGeo;
    private Geometry goalRightGeo;
    //Boolean to check whether or not to start a timer (ie after a goal has been scored). Changes to true when countdown finishes
    private boolean scoredRecently = false;
    //Text to display scores and the coutndownb timer (Which is also used to say "GAME OVER" or "YOU WIN"
    private BitmapText timerText;
    private BitmapText paddle1ScoreText;
    private BitmapText paddle2ScoreText;
    //Picture used on pause menu, mainmenu and difficulty menu. Is used to display Aliens in panic except on difficulty menu
    //where it doubles up as another title
    private Picture titleText;
    //Float used as a timer
    private float timeCount;
    //Boolean to determine if current game is 2Player
    private boolean isTwoPlayer;
    //Boolean used to determine whcih option on the PauseMenu has selected
    private boolean isResumeSelected;
    //Checks if the game is over so update loop can display Game Over screen and then take user back to main menu
    private boolean wonRecently;
    //Floats to determine the delay of AI, in seconds
    private float aiDelay;
    private final float aiDelayEasy = 1.5f;
    private final float aiDelayMedium = .75f;
    private final float aiDelayHard = .3f;
    //Floats to determine the speed of AI
    private float aiSpeed;
    private float speedEasy = 5;
    private float speedMedium = 6.5f;
    private float speedHard = 8;
    //Second float timer used to see if the AIs delay has passed, so it can take action
    private float timeCountDelay;
    //Audio Tracks used in game
    private AudioNode aliensInPanic;
    private AudioNode ambientTrack;
    private AudioNode laserAudio;

    public void simpleInitApp() {
        //Sets background to grey
        viewPort.setBackgroundColor(ColorRGBA.LightGray);

        //Camera starts top-down above the table
        cam.setLocation(new Vector3f(0f, 15f, 0f));
        cam.setRotation(new Quaternion(0.49595132f, -0.50380206f, 0.49447545f, 0.50567764f));

        //Disables camera movement
        flyCam.setEnabled(false);
        
        //Removes FPS and stats from bottom left
        setDisplayStatView(false);
        setDisplayFps(false);
        
        //Game begins in MainMenu state
        state = GameState.mainMenu;

        //Initializes everything used for mainMenu
        initLighting();
        initKeys(state);
        initMenuBackground();
        initMainMenu();
        initGREATESTSONG();
    }

    //Method used to initilize the air hockey table
    private void initTable() {
        //Camera state is set as default, topdown
        camState = CameraState.topDownView;
        //Game is just starting so this must be set back to false
        wonRecently = false;
        //Creates a new spatial using the newTable model
        Spatial tableSpatial = assetManager.loadModel("Models/newtable.j3o");
        //Moves table to middle of the world
        tableSpatial.setLocalTranslation(0, 0, 0);
        //Makes the table appear in the scenegraph
        rootNode.attachChild(tableSpatial);
    }

    //Used to initilize all lighting in the game
    private void initLighting() {
        //AmbientLight used to light everything in scenegraph
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        //Attaches to both rootNode and guiNode in order to light both menus and the main game world
        rootNode.addLight(al);
        guiNode.addLight(al);
    }

    //Initializes audio track for MainMenu
    private void initGREATESTSONG() {
        //Assigns Audio to the AudioNode
        aliensInPanic = new AudioNode(assetManager, "Sounds/Aliens In Panic.wav", false);
        //Makes track loop
        aliensInPanic.setLooping(true);
        //Changes volume
        aliensInPanic.setVolume(3);
        //Plays track
        aliensInPanic.play();
    }

    //Initializes audio track for when game is running
    private void initAmbientTrack() {
        ambientTrack = new AudioNode(assetManager, "Sounds/AmbientTrack.wav", false);
        ambientTrack.setLooping(true);
        ambientTrack.setVolume(3);
        ambientTrack.play();
    }

    //Initializes audio sound effect when puck is hit
    private void initLaserAudio() {
        laserAudio = new AudioNode(assetManager, "Sounds/Laser.wav", false);
        laserAudio.setLooping(false);
        laserAudio.setPositional(true);
        laserAudio.setVolume(2);
        //Attaches AudioNode as it's positional and follows the puck around
        rootNode.attachChild(laserAudio);
    }

    ////Initializes timer text used to countdown after a goal is scored
    private void initTimer() {
        guiNode.detachAllChildren();
        timerText = new BitmapText(guiFont, false);
        timerText.setSize(settings.getHeight() / 10);
        timerText.setColor(ColorRGBA.Blue);
        timerText.setText("");
        timerText.setLocalTranslation(settings.getWidth() / 2, settings.getHeight() / 2, 0);
        guiNode.attachChild(timerText);
    }

    //Initializes scoreboard text at top of screen
    private void initScoreText() {
        paddle1ScoreText = new BitmapText(guiFont, false);
        paddle1ScoreText.setSize(settings.getHeight() / 15);
        paddle1ScoreText.setColor(ColorRGBA.Red);
        paddle1ScoreText.setText("Player 1 : " + paddle1Score);
        paddle1ScoreText.setLocalTranslation(settings.getWidth() / 5f, settings.getHeight() / 1.05f, 0);
        guiNode.attachChild(paddle1ScoreText);

        paddle2ScoreText = new BitmapText(guiFont, true);
        paddle2ScoreText.setSize(settings.getHeight() / 15);
        paddle2ScoreText.setColor(ColorRGBA.Red);
        if (isTwoPlayer == true) {
            paddle2ScoreText.setText(paddle2Score + " : Player 2");
        } else {
            paddle2ScoreText.setText(paddle2Score + " : CPU");
        }
        paddle2ScoreText.setLocalTranslation(settings.getWidth() / 1.7f, settings.getHeight() / 1.05f, 0);
        guiNode.attachChild(paddle2ScoreText);
    }

    //Initializes background of menus
    private void initMenuBackground() {
        //Ensures camera is in correct position for hwen a game starts (top-down)
        cam.setLocation(new Vector3f(0f, 15f, 0f));
        cam.setRotation(new Quaternion(0.49595132f, -0.50380206f, 0.49447545f, 0.50567764f));
        //Gets rid of all gui elements in preparation for new menu being set up
        guiNode.detachAllChildren();
        Picture background = new Picture("Menu Backdrop");
        background.setImage(assetManager, "Textures/MenuBackdrop.jpg", true);
        background.setWidth(settings.getWidth());
        background.setHeight(settings.getHeight());
        guiNode.attachChild(background);
        //Moves it to the back of the GUI scene so buttons can be placed in front
        background.setLocalTranslation(0, 0, -1);
    }

    //Initializes text used for title screen, difficulty screen and pause screen
    //Parameter String is used to determine if we want "Aliens In Panic" or "Difficulty" which is different
    //depending on which menu we're using
    private void initTitleText(String s) {
        titleText = new Picture("titleText");
        //Assigns titleText a picture equal to the name of the string passed ("TitleText" or "DifficultyText")
        titleText.setImage(assetManager, "Textures/" + s + ".png", true);
        titleText.setWidth(settings.getWidth() / 2);
        titleText.setHeight(settings.getHeight() / 10);
        guiNode.attachChild(titleText);
        titleText.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 4), settings.getHeight() / 1.3f, 0);
    }

    //Initializes the 3 main menu buttons, Single player, Multiplayer and exit
    //If image name has "selected" in it, it has a yellow background and is used to tell the user which option they have selected
    private void initMainMenu() {
        menuState = MenuState.onePlayer;
        onePlayerButton = new Picture("singlePlayer");
        //onePlayerButton is selected by default, so use yellow background
        onePlayerButton.setImage(assetManager, "Textures/SinglePlayerSelected.png", true);
        guiNode.attachChild(onePlayerButton);
        onePlayerButton.setHeight(settings.getHeight() / 10);
        onePlayerButton.setWidth(settings.getWidth() / 5);
        onePlayerButton.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 10), settings.getHeight() / 2, 1);


        twoPlayerButton = new Picture("multiplayer");
        twoPlayerButton.setImage(assetManager, "Textures/Multiplayer.png", true);
        guiNode.attachChild(twoPlayerButton);
        twoPlayerButton.setHeight(settings.getHeight() / 10);
        twoPlayerButton.setWidth(settings.getWidth() / 5);
        twoPlayerButton.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 10), settings.getHeight() / 3, 1);

        exitButton = new Picture("exit");
        exitButton.setImage(assetManager, "Textures/ExitButton.png", true);
        guiNode.attachChild(exitButton);
        exitButton.setHeight(settings.getHeight() / 10);
        exitButton.setWidth(settings.getWidth() / 5);
        exitButton.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 10), settings.getHeight() / 6, 1);

        //Calls to initialize title text, helps keep program simpler and reduces repeat code
        initTitleText("TitleText");
    }

    //Similiar to last method, used to initialize the difficulty menu
    private void initDifficultyMenu() {
        menuState = MenuState.easy;
        easyMenu = new Picture("easyMenu");
        //easyMenu button is selected by default
        easyMenu.setImage(assetManager, "Textures/EasySelectedMenu.png", true);
        guiNode.attachChild(easyMenu);
        easyMenu.setHeight(settings.getHeight() / 10);
        easyMenu.setWidth(settings.getWidth() / 5);
        easyMenu.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 10), settings.getHeight() / 2, 1);


        mediumMenu = new Picture("mediumMenu");
        mediumMenu.setImage(assetManager, "Textures/MediumMenu.png", true);
        guiNode.attachChild(mediumMenu);
        mediumMenu.setHeight(settings.getHeight() / 10);
        mediumMenu.setWidth(settings.getWidth() / 5);
        mediumMenu.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 10), settings.getHeight() / 3, 1);

        hardMenu = new Picture("hardMenu");
        hardMenu.setImage(assetManager, "Textures/HardMenu.png", true);
        guiNode.attachChild(hardMenu);
        hardMenu.setHeight(settings.getHeight() / 10);
        hardMenu.setWidth(settings.getWidth() / 5);
        hardMenu.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 10), settings.getHeight() / 6, 1);

        //Calls to initialize title text with text of "Difficulty", helps keep program simpler and reduces repeat code
        initTitleText("DifficultyText");
    }

    //Initialize the pause menu with two buttons, resume and pause, seen in game if the user pauses
    private void initPauseMenu() {
        guiNode.detachAllChildren();
        //sets up a low-opacity PNG that dims the background so the user can still see the game even while it is not active
        Picture pauseBackground = new Picture("Pause Backdrop");
        pauseBackground.setImage(assetManager, "Textures/PauseMenuAlpha.png", true);
        pauseBackground.setWidth(settings.getWidth());
        pauseBackground.setHeight(settings.getHeight());
        guiNode.attachChild(pauseBackground);
        //Sends to back so buttons aren't dimmed
        pauseBackground.setLocalTranslation(0, 0, -1);


        resumePauseMenu = new Picture("Resume");
        resumePauseMenu.setImage(assetManager, "Textures/ResumeSelectedPauseMenu.png", true);
        guiNode.attachChild(resumePauseMenu);
        resumePauseMenu.setHeight(settings.getHeight() / 10);
        resumePauseMenu.setWidth(settings.getWidth() / 5);
        resumePauseMenu.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 10), settings.getHeight() / 3, 1);

        //This menu uses a single boolean rather than menuState as there are only two buttons
        //If resume isn't selected, then exit is, if resume is selected then exit isn't
        isResumeSelected = true;

        exitPauseMenu = new Picture("exitPauseMenu");
        exitPauseMenu.setImage(assetManager, "Textures/ExitButton.png", true);
        guiNode.attachChild(exitPauseMenu);
        exitPauseMenu.setHeight(settings.getHeight() / 10);
        exitPauseMenu.setWidth(settings.getWidth() / 5);
        exitPauseMenu.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 10), settings.getHeight() / 6, 1);
    }

    //Creates the boxes for detecting if a player has scored
    private void initGoals() {
        //Game has just started so no-one has recently scored, so set to false
        scoredRecently = false;
        Box b = new Box(1.85f, .2f, .1f);
        goalLeftGeo = new Geometry("Box", b);
        //Moves goal shape to goal area
        goalLeftGeo.setLocalTranslation(.2f, .3f, 7.2f);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        mat.setColor("Color", ColorRGBA.Blue);   // set color of material to blue
        goalLeftGeo.setMaterial(mat);
        //Creates a bounding box for detecting collisions with the puck
        goalLeft = new BoundingBox();
        goalLeftGeo.setModelBound(goalLeft);
        goalLeftGeo.updateModelBound();

        goalRightGeo = new Geometry("Box2", b);
        goalRightGeo.setLocalTranslation(.2f, .3f, -7.2f);
        goalRightGeo.setMaterial(mat);
        goalRight = new BoundingBox();
        goalRightGeo.setModelBound(goalRight);
        goalRightGeo.updateModelBound();

        //Moves the BoundingBoxes on top of their corresponding geometries
        goalLeft.setCenter(goalLeftGeo.getLocalTranslation());
        goalRight.setCenter(goalRightGeo.getLocalTranslation());
    }

    //Creates paddles for use in game
    private void initPaddles() {
        //Resets score back to 0
        paddle1Score = 0;
        //Loads model for paddle
        paddle1 = assetManager.loadModel("Models/mallet.j3o");
        //Moves to left side of table
        paddle1.setLocalTranslation(new Vector3f(0, 0.3f, 3f));
        //Creates a boundingsphere and sticks it to the paddle
        paddle1_hitbox = new BoundingSphere();
        paddle1.setModelBound(paddle1_hitbox);
        paddle1.setLocalScale(.4f);
        rootNode.attachChild(paddle1);

        paddle2Score = 0;
        paddle2 = assetManager.loadModel("Models/mallet.j3o");
        paddle2.setLocalTranslation(new Vector3f(0, .3f, -3.1f));
        //Creates a boundingsphere and sticks it to the paddle
        paddle2_hitbox = new BoundingSphere();
        paddle2.setModelBound(paddle2_hitbox);
        paddle2.setLocalScale(.4f);
        rootNode.attachChild(paddle2);

        //Paddles should not be moving to set directions back to 0
        paddle1Direction = new Vector3f(0, 0, 0);
        paddle2Direction = new Vector3f(0, 0, 0);
    }

    //Creates geometry and bounds for puck
    private void initPuck() {

        puck = assetManager.loadModel("Models/mallet.j3o");
        puck.setLocalScale(.2f);
        puck.setLocalTranslation(new Vector3f(0f, .3f, 0));
        //Creates a boundingsphere and sticks it to the puck
        puck_hitbox = new BoundingSphere(.1f, puck.getWorldTranslation());
        puck.setModelBound(puck_hitbox);
        puck.updateGeometricState();
        rootNode.attachChild(puck);

        //Sets direction and speed to corresponding UserDatas
        puck.setUserData("direction", new Vector3f(.5f, 0, .5f));
        puck.setUserData("speed", 2f);
    }

    //Initializes keys for current state
    private void initKeys(GameState s) {
        //Ensures all mappings from previous states are removed
        inputManager.clearMappings();
        inputManager.clearRawInputListeners();
        if (s == GameState.mainMenu) {  //If user is currently on MainMenu, declare mappings for moving up/down menu and selecting an option
            inputManager.addMapping("UpMenu", new KeyTrigger(KeyInput.KEY_UP));
            inputManager.addMapping("DownMenu", new KeyTrigger(KeyInput.KEY_DOWN));
            inputManager.addMapping("SpaceMenu", new KeyTrigger(KeyInput.KEY_SPACE));
            //Adds to actionListeneer
            inputManager.addListener(actionListener, "UpMenu", "DownMenu", "SpaceMenu");
        } else if (s == GameState.gamePaused) {     //If game is paused, declare mappings for moving up/down, selecting an option and resuming game through ESC
            inputManager.addMapping("Pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
            inputManager.addMapping("UpMenu", new KeyTrigger(KeyInput.KEY_UP));
            inputManager.addMapping("DownMenu", new KeyTrigger(KeyInput.KEY_DOWN));
            inputManager.addMapping("SpaceMenu", new KeyTrigger(KeyInput.KEY_SPACE));
            inputManager.addListener(actionListener, "UpMenu", "DownMenu", "SpaceMenu", "Pause");
        } else if (s == GameState.gameRunning) {    //If game is running...
            //Declare mapping for pause and switching camera angles (tab) as these are used in all versions of GameRunning
            inputManager.addMapping("Pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
            inputManager.addMapping("Tab", new KeyTrigger(KeyInput.KEY_TAB));
            inputManager.addListener(actionListener, "Tab", "Pause");

            if (camState == CameraState.paddle1View) {  //Camera is currently from paddle1 Point of view, so movement keys need to be switched
                inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_S));
                inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_W));
                inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_A));
                inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_D));

                inputManager.addListener(analogListener, "Left", "Right", "Up", "Down");
            } else {    //Else camera is top-down or side view and use the same bindings
                inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
                inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
                inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
                inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));

                inputManager.addListener(analogListener, "Left", "Right", "Up", "Down");

                //Next if statement is nested within top-down or side view as the user cannot be 2 player and in paddle1view
                if (isTwoPlayer == true) {  //declares mappings for paddle2 ONLY if game is in two player mode.
                    inputManager.addMapping("Left2", new KeyTrigger(KeyInput.KEY_LEFT));
                    inputManager.addMapping("Right2", new KeyTrigger(KeyInput.KEY_RIGHT));
                    inputManager.addMapping("Up2", new KeyTrigger(KeyInput.KEY_UP));
                    inputManager.addMapping("Down2", new KeyTrigger(KeyInput.KEY_DOWN));
                    inputManager.addListener(analogListener, "Left2", "Right2", "Up2", "Down2");
                }
            }
        }

    }
    //Action listener for one time events
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Pause") && !keyPressed) {  //if user presses pause button...
                if (state == GameState.gameRunning) {   //if game is currently running...
                    state = GameState.gamePaused;       //Pause the game
                    initPauseMenu();        //then initialize pause menu, re-initilize keys for correct mappings, and initilize the title text
                    initKeys(state);
                    initTitleText("TitleText");
                } else if (state == GameState.gamePaused) { //Game was already paused so remove pause menu and and set game to running again
                    state = GameState.gameRunning;
                    guiNode.detachAllChildren();
                }
            }
            if (name.equals("Tab") && !keyPressed) {        //if user tries to switch camera angles...
                if (camState == CameraState.topDownView) {  //If camera is currently in top-down, switch to side view
                    cam.setLocation(new Vector3f(10.34306f, 8.431353f, -0.061023343f));
                    cam.setRotation(new Quaternion(0.2318152f, -0.6683752f, 0.23240772f, 0.667475f));
                    camState = CameraState.sideView;
                } else if (camState == CameraState.sideView) {  //else if camera is in sideview...
                    if (isTwoPlayer == true) {  //check if game is twoplayer - if it is, then back to top down
                        cam.setLocation(new Vector3f(0f, 15f, 0f));
                        cam.setRotation(new Quaternion(0.49595132f, -0.50380206f, 0.49447545f, 0.50567764f));
                        camState = CameraState.topDownView;
                    } else {    //else game is singleplayer, so user switches to paddle1view
                        cam.setLocation(new Vector3f(-0.09945696f, 5.007021f, 12.445462f));
                        cam.setRotation(new Quaternion(6.8808054E-5f, 0.9712846f, -0.23792005f, -4.9522246E-4f));
                        camState = CameraState.paddle1View;
                        initKeys(state);    //has to re-initialize keys as angle is switched
                    }
                } else if (camState == CameraState.paddle1View) {   //else camera is in paddle1view so switch back to topdown
                    cam.setLocation(new Vector3f(0f, 15f, 0f));
                    cam.setRotation(new Quaternion(0.49595132f, -0.50380206f, 0.49447545f, 0.50567764f));
                    camState = CameraState.topDownView;
                    initKeys(state);
                }
            }

            if (name.equals("UpMenu") && !keyPressed) { //if user is on a menu and presses up
                if (state == GameState.mainMenu) {  //If user is on mainmenu, switch between menuStates that exist on mainMenu
                    if (menuState == MenuState.onePlayer) { //if menuState is oneplayer, change graphic of selected button to button above
                        menuState = MenuState.exit; //and change menuState to new selected menu
                        onePlayerButton.setImage(assetManager, "Textures/SinglePlayer.png", true);
                        exitButton.setImage(assetManager, "Textures/ExitButtonSelected.png", true);
                    } else if (menuState == MenuState.twoPlayer) { //if menuState is twoPlayer, change graphic of selected button to button above
                        menuState = MenuState.onePlayer;
                        twoPlayerButton.setImage(assetManager, "Textures/Multiplayer.png", true);
                        onePlayerButton.setImage(assetManager, "Textures/SinglePlayerSelected.png", true);
                    } else if (menuState == MenuState.exit) {//if menuState is exit, change graphic of selected button to button above
                        menuState = MenuState.twoPlayer;
                        exitButton.setImage(assetManager, "Textures/ExitButton.png", true);
                        twoPlayerButton.setImage(assetManager, "Textures/MultiplayerSelected.png", true);
                    } else if (menuState == MenuState.easy) {//if menuState is easy (on the difficulty menu), change graphic of selected button to button above
                        menuState = MenuState.hard;
                        easyMenu.setImage(assetManager, "Textures/EasyMenu.png", true);
                        hardMenu.setImage(assetManager, "Textures/HardSelectedMenu.png", true);
                    } else if (menuState == MenuState.medium) { //if menuState is medium (on the difficulty menu), change graphic of selected button to button above
                        menuState = MenuState.easy;
                        mediumMenu.setImage(assetManager, "Textures/MediumMenu.png", true);
                        easyMenu.setImage(assetManager, "Textures/EasySelectedMenu.png", true);
                    } else if (menuState == MenuState.hard) { //if menuState is hard (on the difficulty menu), change graphic of selected button to button above
                        menuState = MenuState.medium;
                        hardMenu.setImage(assetManager, "Textures/HardMenu.png", true);
                        mediumMenu.setImage(assetManager, "Textures/MediumSelectedMenu.png", true);
                    }
                } else if (state == GameState.gamePaused) { //If game is paused
                    if (isResumeSelected == true) { //is resume is currently selected, switch graphics so correct graphic is selected
                        resumePauseMenu.setImage(assetManager, "Textures/ResumePauseMenu.png", true);
                        exitPauseMenu.setImage(assetManager, "Textures/ExitButtonSelected.png", true);
                        isResumeSelected = false;
                    } else {    //switch resume graphic to selected version
                        resumePauseMenu.setImage(assetManager, "Textures/ResumeSelectedPauseMenu.png", true);
                        exitPauseMenu.setImage(assetManager, "Textures/ExitButton.png", true);
                        isResumeSelected = true;
                    }
                }
            }
            if (name.equals("DownMenu") && !keyPressed) {   //Same as method above, but for down
                if (state == GameState.mainMenu) {
                    if (menuState == MenuState.onePlayer) {
                        menuState = MenuState.twoPlayer;
                        onePlayerButton.setImage(assetManager, "Textures/SinglePlayer.png", true);
                        twoPlayerButton.setImage(assetManager, "Textures/MultiplayerSelected.png", true);
                    } else if (menuState == MenuState.twoPlayer) {
                        menuState = MenuState.exit;
                        twoPlayerButton.setImage(assetManager, "Textures/Multiplayer.png", true);
                        exitButton.setImage(assetManager, "Textures/ExitButtonSelected.png", true);
                    } else if (menuState == MenuState.exit) {
                        menuState = MenuState.onePlayer;
                        exitButton.setImage(assetManager, "Textures/ExitButton.png", true);
                        onePlayerButton.setImage(assetManager, "Textures/SinglePlayerSelected.png", true);
                    } else if (menuState == MenuState.easy) {
                        menuState = MenuState.medium;
                        mediumMenu.setImage(assetManager, "Textures/MediumSelectedMenu.png", true);
                        easyMenu.setImage(assetManager, "Textures/EasyMenu.png", true);
                    } else if (menuState == MenuState.medium) {
                        menuState = MenuState.hard;
                        hardMenu.setImage(assetManager, "Textures/HardSelectedMenu.png", true);
                        mediumMenu.setImage(assetManager, "Textures/MediumMenu.png", true);
                    } else if (menuState == MenuState.hard) {
                        menuState = MenuState.easy;
                        easyMenu.setImage(assetManager, "Textures/EasySelectedMenu.png", true);
                        hardMenu.setImage(assetManager, "Textures/HardMenu.png", true);
                    }
                } else if (state == GameState.gamePaused) {
                    if (isResumeSelected == true) {
                        resumePauseMenu.setImage(assetManager, "Textures/ResumePauseMenu.png", true);
                        exitPauseMenu.setImage(assetManager, "Textures/ExitButtonSelected.png", true);
                        isResumeSelected = false;
                    } else {
                        resumePauseMenu.setImage(assetManager, "Textures/ResumeSelectedPauseMenu.png", true);
                        exitPauseMenu.setImage(assetManager, "Textures/ExitButton.png", true);
                        isResumeSelected = true;
                    }
                }
            }

            if (name.equals("SpaceMenu") && !keyPressed) {
                if (state == GameState.mainMenu) {  //if current state is main menu...
                    if (menuState == MenuState.exit) {//if user has exit selected, then exit
                        System.exit(0); //Exits game (not clean, but couldn't get app.stop working)
                    }
                    if (menuState == MenuState.twoPlayer) { //If twoPlayer is selected, jump straight into game
                        isTwoPlayer = true;
                        state = GameState.gameRunning;
                        guiNode.detachAllChildren(); //gets rid of menu
                        //Initilizes everything needed for gamerunning mode
                        initPuck();
                        initGoals();
                        initTable();
                        initTimer();
                        initPaddles();
                        initScoreText();
                        initKeys(state);
                        aliensInPanic.stop();   //stops menu audio track
                        initAmbientTrack();     //Starts gamerunning audio track (Includes playing)
                        initLaserAudio();       //Initilizes laser SFX, but doesn't play
                    } else if (menuState == MenuState.onePlayer) {  //user selected singleplayer, so take them to difficulty menu
                        isTwoPlayer = false;
                        guiNode.detachAllChildren();

                        //Creates difficulty menu
                        initMenuBackground();
                        initDifficultyMenu();

                    } else {    //Covers any option selected on difficulty menu
                        state = GameState.gameRunning;
                        guiNode.detachAllChildren();
                        //Initializes everything needed for game
                        initPuck();
                        initGoals();
                        initTable();
                        initTimer();
                        initPaddles();
                        initScoreText();
                        initKeys(state);
                        aliensInPanic.stop();   //Stops menu audio
                        initAmbientTrack();     //starts game running audio
                        initLaserAudio();       //prepares laser sfx
                        if (menuState == MenuState.easy) {  //If user seletected easy...
                            aiSpeed = speedEasy;    //use constant for ai speed for easy bot
                            aiDelay = aiDelayEasy;  //use constant for ai delay for easy bot
                        } else if (menuState == MenuState.medium) { //if user selected medium, do the same but with med constants
                            aiSpeed = speedMedium;
                            aiDelay = aiDelayMedium;
                        } else { //user selected hard, so do the same but with hard constants
                            aiSpeed = speedHard;
                            aiDelay = aiDelayHard;
                        }
                    }
                } else if (state == GameState.gamePaused) { //if user presses space in pause menu
                    if (isResumeSelected == true) { //if resume is selected, resume game and delete menu
                        state = GameState.gameRunning;
                        guiNode.detachAllChildren();
                    } else {    //else go back to main menu
                        state = GameState.mainMenu;
                        //delete current menu and game in session
                        rootNode.detachAllChildren();
                        guiNode.detachAllChildren();
                        initMenuBackground();
                        initMainMenu();
                        ambientTrack.stop();    //stop in-game audio
                        aliensInPanic.play();   //and play menu audio
                    }
                }
            }
        }
    };
    //AnalogListener used for controlling paddles
    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            if (name.equals("Right")) { //if user moves right
                paddle1Direction.z = -1;    //change direction to go in the direction to the right
            }
            if (name.equals("Left")) {  //if user moves left, do same but for direction to the left
                paddle1Direction.z = 1;
            }
            if (name.equals("Up")) {    //if user moves up, do same but for direction to the left
                paddle1Direction.x = -1;
            }
            if (name.equals("Down")) {  //if user moves down, do same but for direction to the left
                paddle1Direction.x = 1;
            }

            //Same for paddle2
            if (name.equals("Right2")) {
                paddle2Direction.z = -1;
            }
            if (name.equals("Left2")) {
                paddle2Direction.z = 1;
            }
            if (name.equals("Up2")) {
                paddle2Direction.x = -1;
            }
            if (name.equals("Down2")) {
                paddle2Direction.x = 1;
            }
        }
    };

    public void simpleUpdate(float tpf) {
        if (state == GameState.mainMenu) {  // if gameState is on Menu - do nothing!
        } else if (state == GameState.gameRunning) {    //else if game is currently running, do this long, long block of code
            if (wonRecently == true) {  //Game finished recently, so we want to get out of running state and back to menu after a pause
                //Pause the screen on scoreboard which was set up at the end of last loop
                //This has to be in a new loop of update as rendering happens at the end of the loop
                //the game would just pause the frame before i wanted to pause
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                //Sets game back to main menu, which includes getting rid of scoreboard, game elements and reinitializing menu elements
                state = GameState.mainMenu;
                rootNode.detachAllChildren();
                guiNode.detachAllChildren();
                initMenuBackground();
                initMainMenu();
                initKeys(state);
                ambientTrack.stop();    //pauses in-game audio
                initGREATESTSONG();     //and re-initializes and plays menu audio
            } else if (scoredRecently == true && (paddle1Score < 5 && paddle2Score < 5)) {  //if someone scored but no-one has reached the score limit (5)
                timeCount += tpf;   //adds time per frame to a counter, giving an accurate representation of how much time has elapsed
                if (timeCount > 3) {    //if 3 secs has passed, it's the end of the timer so let the players play again
                    timerText.setText("");  //removes countdown from screen
                    scoredRecently = false; //allows game loop to progress as game is running
                    guiNode.attachChild(timerText);
                } else if (timeCount > 2) { //if 2 seconds has passed
                    timerText.setText("1"); //display 1 as there is 1 seconds till users are in control
                    guiNode.attachChild(timerText);
                } else if (timeCount > 1) {//if 1 seconds has passed
                    timerText.setText("2");//display 2 as there is 2 seconds till users are in control
                    guiNode.attachChild(timerText);
                } else { //if <1 second has passed
                    timerText.setText("3"); //display 3 as there is 3 seconds till users are in control
                    guiNode.attachChild(timerText);
                }
            } else {
                timeCount = 0;  //resets timer back to 0 for next time
                puck_hitbox.setCenter(puck.getLocalTranslation());  //Moves the boundingSphere onto the puck spatial (This should be done automatically by jme3 but it wouldn't work for me)
                Vector3f puckDirection = puck.getUserData("direction"); //gets current direction of puck and puts into variable for easy access
                float puckSpeed = puck.getUserData("speed");    //gets current speed of puck and puts into variable for easy access
                Vector3f puckLoc = puck.getLocalTranslation();  //gets puck location into a variable for ease of use
                Vector3f paddle1Loc = paddle1.getLocalTranslation();    //gets paddle1 location into a variable for ease of use
                Vector3f paddle2Loc = paddle2.getLocalTranslation();    //gets paddle2 location into a variable for ease of use
                laserAudio.setLocalTranslation(puckLoc);    //sets the laserAudioNode to the location of puck, for positional audio



                //Applies friction to paddles
                if (paddle1Direction.x > 0) {
                    paddle1Direction.x -= friction * tpf;
                } else if (paddle1Direction.x < 0) {
                    paddle1Direction.x += friction * tpf;
                }
                if (paddle1Direction.z > 0) {
                    paddle1Direction.z -= friction * tpf;
                } else if (paddle1Direction.z < 0) {
                    paddle1Direction.z += friction * tpf;
                }

                //if paddle.x is between 0 and friction then change direction to 0 (prevents paddle from going to floats like 0.00000000000000000001
                if ((paddle1Direction.x > 0 && paddle1Direction.x < friction) || (paddle1Direction.x < 0 && paddle1Direction.x > -friction)) {
                    paddle1Direction.x = 0;
                }
                //if paddle.z is between 0 and friction then change direction to 0 (prevents paddle from going to floats like 0.00000000000000000001
                if ((paddle1Direction.z > 0 && paddle1Direction.z < friction) || (paddle1Direction.z < 0 && paddle1Direction.z > -friction)) {
                    paddle1Direction.z = 0;
                }

                //Applies friction to paddle2
                if (paddle2Direction.x > 0) {
                    paddle2Direction.x -= friction * tpf;
                } else if (paddle2Direction.x < 0) {
                    paddle2Direction.x += friction * tpf;
                }
                if (paddle2Direction.z > 0) {
                    paddle2Direction.z -= friction * tpf;
                } else if (paddle2Direction.z < 0) {
                    paddle2Direction.z += friction * tpf;
                }

                //stops paddle if going too slowly
                if ((paddle2Direction.x > 0 && paddle2Direction.x < friction) || (paddle2Direction.x < 0 && paddle2Direction.x > -friction)) {
                    paddle2Direction.x = 0;
                }
                if ((paddle2Direction.z > 0 && paddle2Direction.z < friction) || (paddle2Direction.z < 0 && paddle2Direction.z > -friction)) {
                    paddle2Direction.z = 0;
                }

                //Applies friction to puck
                puckSpeed -= puckFriction * tpf;

                //if puck speed gets too low, change it to 0, brining to a full halt
                if (puckSpeed < 0.0001f) {
                    puckSpeed = 0;
                }


                //Collision detecting and reaction code
                CollisionResults results = new CollisionResults();
                paddle1.collideWith(puck_hitbox, results);  //Checks for a collision between paddle1 and puck
                if (results.size() > 0) {   //if collision found
                    if (paddle1HasTouch == false) { //if paddle1 has not touched it within the last frame or 2

                        //gets the angle of the puck by using trigonometry with the distance between the entities along the
                        //x axis and along the z axis
                        float puckAngle = (float) FastMath.atan2((puckLoc.z + 0.012963653f) - (paddle1Loc.z + 1.001358E-5f), (puckLoc.x + 0.012963653f) - (paddle1Loc.x + 1.001358E-5f));
                        puckDirection.x += (float) FastMath.cos(puckAngle); //calculates how far along the x axis the puck should be going using the angle above
                        puckDirection.z += (float) FastMath.sin(puckAngle);//calculates how far along the z axis the puck should be going using the angle above
                        puckSpeed = newPuckSpeed;   //Changes the speed to the speed after collision

                        //Checks if the pucks direction should be reversed along the x axis or not
                        if (puckDirection.x < 0 && paddle1Loc.x < puckLoc.x) {
                            puckDirection.x *= -1f;
                        } else if (puckDirection.x > 0 && paddle1Loc.x > puckLoc.x) {
                            puckDirection.x *= -1f;
                        }

                        //Checks if the pucks direction should be reversed along the z axis or not
                        if (puckDirection.z < 0 && paddle1Loc.z < puckLoc.z) {
                            puckDirection.z *= -1f;
                        } else if (puckDirection.z > 0 && paddle1Loc.z > puckLoc.z) {
                            puckDirection.z *= -1f;
                        }
                        //Paddle1 has just deflected a collision so set paddle1HasTouch to true
                        paddle1HasTouch = true;
                        //both can't have collided at same time so set paddle2 has touch to false
                        paddle2HasTouch = false;
                        laserAudio.play();  //Plays laser sound effect
                    } else {
                        puckSpeed = newPuckSpeed;   //puck mayh be inside of paddle, so just change speed
                    }
                    puck.move(puckDirection.mult(tpf * puckSpeed));
                } else {
                    paddle1HasTouch = false;    //No collision, so this is now false
                }

                results.clear();    //clears results from previous collision

                //Same code as for paddle1, but for paddle2
                paddle2.collideWith(puck_hitbox, results);
                if (results.size() > 0) {
                    if (paddle2HasTouch == false) {
                        float puckAngle = (float) FastMath.atan2((puckLoc.z + 0.012963653f) - (paddle2Loc.z + 1.001358E-5f), (puckLoc.x + 0.012963653f) - (paddle2Loc.x + 1.001358E-5f));
                        puckDirection.x += (float) FastMath.cos(puckAngle);
                        puckDirection.z += (float) FastMath.sin(puckAngle);
                        puckSpeed = newPuckSpeed;
                        if (puckDirection.x < 0 && paddle2Loc.x < puckLoc.x) {
                            puckDirection.x *= -1f;
                        } else if (puckDirection.x > 0 && paddle2Loc.x > puckLoc.x) {
                            puckDirection.x *= -1f;
                        }

                        if (puckDirection.z < 0 && paddle2Loc.z < puckLoc.z) {
                            puckDirection.z *= -1f;
                        } else if (puckDirection.z > 0 && paddle2Loc.z > puckLoc.z) {
                            puckDirection.z *= -1f;
                        }
                        paddle1HasTouch = false;
                        paddle2HasTouch = true;
                        laserAudio.play();
                    } else {
                        puckSpeed = newPuckSpeed;
                    }
                    puck.move(puckDirection.mult(tpf * puckSpeed));
                } else {
                    paddle2HasTouch = false;
                }

                if (isTwoPlayer == false) { //if game is not two player, then calculate AI movements...
                    if (paddle1HasTouch == true) {  //if player1 has just touched the puck, then begin the delay (aka reaction time)
                        timeCountDelay = 0; //coounter starts at 0
                    }
                    timeCountDelay += tpf;  //increments timer

                    if (timeCountDelay > aiDelay) { //if reaction delay has passed, calculate movements
                        //gets angle from paddle2 to puck
                        float angle = (float) FastMath.atan2((puckLoc.z + 0.012963653f) - (paddle2Loc.z + 1.001358E-5f), (puckLoc.x + 0.012963653f) - (paddle2Loc.x + 1.001358E-5f));
                        paddle2Direction.x = (float) FastMath.cos(angle);   //keeps AI aligned with the puck on the x axis
                        if (puckLoc.z < -.41f) {    //if puck is within the bots half of the table
                            if (puckDirection.x < 0 && (paddle2Loc.z - puckLoc.z) < 0.5f) { //If puck is close enough to hit
                                paddle2Direction.z = (float) FastMath.sin(angle);   //Move paddle2 along z axis to hit the puck
                            } else if (puckDirection.z == 0) {  //if pucks direction along the z axis is dead (stuck in AIs half)
                                paddle2Direction.z = (float) FastMath.sin(angle);//Move paddle2 along z axis to hit the puck
                            } else if (puckLoc.z < paddle2Loc.z) {  //if puck location is behind the AI then
                                paddle2Direction.z = (float) FastMath.sin(angle);//Move paddle2 along z axis to hit the puck
                            } else if (puckSpeed == 0) {    //if puck is not moving
                                paddle2Direction.z = (float) FastMath.sin(angle);//Move paddle2 along z axis to hit the puck
                            }
                            if ((puckLoc.x - paddle2Loc.x) < 0.01 && (puckLoc.z - paddle2Loc.z) < 0.01) {
                                //this if statement tries to solve a collision issue where the puck gets stuck inside the AI paddle
                                paddle2Direction.z = (float) FastMath.sin(angle) * 5;
                            }
                        } else if (paddle2Loc.z > -3) { //if paddle is out of place on the z axis and puck is on their side
                            paddle2Direction.z = (float) -(FastMath.sin(angle));    //move back to default z axis position
                        }
                    }
                }

                //Boundaries of table

                //if puckLoc.x goes past boundaries, direction is reversed
                if (puckLoc.x > 3.55) {
                    puckLoc.x = 3.55f;
                    puckDirection.x *= -1;
                } else if (puckLoc.x < -3.55) {
                    puckLoc.x = -3.55f;
                    puckDirection.x *= -1;
                }
                if (puckLoc.z > 6.9f) {
                    puckLoc.z = 6.9f;
                    puckDirection.z *= -1;
                } else if (puckLoc.z < -6.9) {
                    puckLoc.z = -6.9f;
                    puckDirection.z *= -1;
                }

                //if paddle1 tries to go past table boundaries, if statements simply move it back to the boundary
                if (paddle1Loc.x > 3.3) {
                    paddle1Loc.x = 3.3f;
                } else if (paddle1Loc.x < -3.3) {
                    paddle1Loc.x = -3.3f;
                }
                if (paddle1Loc.z > 6.35f) {
                    paddle1Loc.z = 6.35f;
                } else if (paddle1Loc.z < 0.41f) {
                    paddle1Loc.z = .41f;
                }



                //if paddle2 tries to go past table boundaries, if statements simply move it back to the boundary
                if (paddle2Loc.x > 3.3) {
                    paddle2Loc.x = 3.3f;
                } else if (paddle2Loc.x < -3.3) {
                    paddle2Loc.x = -3.3f;
                }
                if (paddle2Loc.z > -0.41) {
                    paddle2Loc.z = -0.41f;
                } else if (paddle2Loc.z < -6.35f) {
                    paddle2Loc.z = -6.35f;
                }

                paddle1.setLocalTranslation(paddle1Loc);    //Updates paddle1 position to whatever has changed in the current update loop
                paddle2.setLocalTranslation(paddle2Loc);    //Updates paddle1 position to whatever has changed in the current update loop

                //Updates direction and speed of puck to whatever was changed in current iteration of update loop
                puck.setUserData("direction", puckDirection);
                puck.setUserData("speed", puckSpeed);

                puck.move(puckDirection.mult(tpf * puckSpeed)); //Moves puck along the direction vector
                paddle1.move(paddle1Direction.mult(tpf * paddle1Speed));    //Moves paddle1 along the direction vector
                if (isTwoPlayer == true) {
                    paddle2.move(paddle2Direction.mult(tpf * paddle2Speed)); //Moves paddle2 along the direction vector
                } else {
                    paddle2.move(paddle2Direction.mult(tpf * aiSpeed));     //Moves paddle2 along the direction vector, but using aiSpeed since this is done when not 2player
                }

                //Collision checking and response for left goal
                results.clear();
                puck.collideWith(goalLeft, results);    //checks for collision
                if (results.size() > 0) {
                    paddle2Score++; //paddle2 scored so increment their score
                    if (isTwoPlayer == true) {  //if twoPlayer, print "Player 2" on scoreboard - if not, print "CPU"
                        paddle2ScoreText.setText(paddle2Score + " : Player 2");
                    } else {
                        paddle2ScoreText.setText(paddle2Score + " : CPU");
                    }
                    puck.setLocalTranslation(.01f, 0.3f, 1f);   //moves puck to side of loser
                    puck.setUserData("direction", new Vector3f(0, 0, 0));   //kills direction of puck
                    paddle1.setLocalTranslation(new Vector3f(0, 0.3f, 3f)); //resets position of paddle1
                    paddle2.setLocalTranslation(new Vector3f(0, .3f, -3.1f));   //resets position of paddle2
                    scoredRecently = true;  //makes this true so the timer can start on next loop
                }

                //Collision checking and response for left goal
                results.clear();
                puck.collideWith(goalRight, results);
                if (results.size() > 0) {
                    paddle1Score++;
                    paddle1ScoreText.setText("Player 1 : " + paddle1Score);
                    puck.setLocalTranslation(.01f, 0.3f, -1f);
                    puck.setUserData("direction", new Vector3f(0, 0, 0));
                    paddle1.setLocalTranslation(new Vector3f(0, 0.3f, 3f));
                    paddle2.setLocalTranslation(new Vector3f(0, .3f, -3.1f));
                    scoredRecently = true;
                }

                if (paddle1Score >= 5) {    //if paddle1 has reached the score limit (won the game)
                    paddle1ScoreText.setLocalTranslation(settings.getWidth() / 5f,
                            settings.getHeight() / 3, 1);   //move score into middle
                    paddle2ScoreText.setLocalTranslation(settings.getWidth() / 1.7f,
                            settings.getHeight() / 3, 1);   //move score into middle

                    //text changed here uses timerText as it is no longer used and saves having another variable
                    if (isTwoPlayer == true) { //if two player
                        timerText.setText("Player 1 Wins!");    //print player1 wins
                    } else {
                        timerText.setText("You Win!");  //else print you win (helps clairfy between winner rather than just "game over"
                    }
                    //Moves timer into middle
                    timerText.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 5f), settings.getHeight() / 1.5f, 1);
                    wonRecently = true; //used in next update loop so the program will switch back to the main menu
                } else if (paddle2Score >= 5) { //if paddle2 has reached the score limit (won the game)
                    paddle1ScoreText.setLocalTranslation(settings.getWidth() / 5f,
                            settings.getHeight() / 3, 1);
                    paddle2ScoreText.setLocalTranslation(settings.getWidth() / 1.7f,
                            settings.getHeight() / 3, 1);
                    if (isTwoPlayer == true) {
                        timerText.setText("Player 2 Wins!");    //print "player 2 wins" if paddle2 wins in a two player game
                    } else {
                        timerText.setText("GAME OVER"); //print "game over" if player loses agianst AI
                    }
                    timerText.setLocalTranslation(settings.getWidth() / 2 - (settings.getWidth() / 5f), settings.getHeight() / 1.5f, 1);
                    wonRecently = true;
                }
            }
        } else if (state == GameState.gamePaused) {
            //game is paused - do nothing!
        }
    }
}
