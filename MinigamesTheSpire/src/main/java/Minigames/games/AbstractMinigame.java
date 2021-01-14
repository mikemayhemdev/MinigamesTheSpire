package Minigames.games;

import Minigames.games.input.bindings.BindingGroup;
import Minigames.patches.Input;
import basemod.interfaces.TextReceiver;
import basemod.patches.com.megacrit.cardcrawl.helpers.input.ScrollInputProcessor.TextInput;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import static Minigames.Minigames.makeGamePath;


//When option to play a game is chosen:
/*
    Minigame area will enter screen in some way. Fade in, and fade out rest of screen?
    Valid controls will be displayed for 3 seconds, then fade out for the last second.
    Minigame will begin.
 */
public abstract class AbstractMinigame implements TextReceiver {
    //Game stuff
    protected boolean isPlaying;
    protected boolean isDone;
    protected boolean blockingInput;

    protected float time = 0;

    protected int phase = -2;
    //-2: Minigame area fading in.
    //-1: Displaying controls.
    //0 and up: Whatever you want. Use a switch statement, just stay at 0, doesn't really matter.

    //Rendering stuff
    //640x640
    public static final int SIZE = 640;

    protected int x = Settings.WIDTH / 2, y = Settings.HEIGHT / 2;
    protected float scale = 0.0f;
    protected float angle = 0.0f;

    private final Color c;

    //background
    private static final int BG_SIZE = 648;
    private Texture background;

    public AbstractMinigame()
    {
        isPlaying = false;
        isDone = false;
        blockingInput = false;

        c = Color.WHITE.cpy();
    }

    //load necessary assets, if any
    public void initialize() {
        isPlaying = true;

        TextInput.startTextReceiver(this);
        blockingInput = true;

        BindingGroup b = getBindings();
        b.allowEsc();
        Input.setBindings(b);
        background = ImageMaster.loadImage(makeGamePath("tempBG.png"));
        transformScale(getMaxScale(), Settings.FAST_MODE ? 0.5f : 1.0f);
    }

    //dispose of loaded assets, if any
    public void dispose() {
        background.dispose();
        Input.clearBindings();
        TextInput.stopTextReceiver(this);
    }

    public boolean playing() {
        return isPlaying;
    }

    public boolean gameDone() {
        return isDone;
    }

    //will be called as long as isPlaying is true
    public void update(float elapsed) {
        if (CardCrawlGame.isPopupOpen || AbstractDungeon.screen != AbstractDungeon.CurrentScreen.NONE) {
            if (blockingInput)
            {
                blockingInput = false;
                TextInput.stopTextReceiver(this);
            }
        }
        else if (!blockingInput)
        {
            blockingInput = true;
            TextInput.startTextReceiver(this);
        }

        Input.update(elapsed);

        if (scaleProgress < scaleTime)
        {
            scaleProgress += elapsed;
            setScale(Interpolation.linear.apply(initialScale, targetScale, Math.min(1, scaleProgress / scaleTime)));
        }

        switch (phase) {
            case -2:
                time += elapsed;

                //c.a = Math.min(1, time * (Settings.FAST_MODE ? 2.0f : 1.0f));

                if (time > (Settings.FAST_MODE ? 0.5f : 1.0f))
                {
                    time = 0;
                    phase = -1;
                }
                break;
            case -1:
                time += elapsed;

                if (time > (Settings.FAST_MODE ? 2.5f : 4.0f))
                {
                    time = 0;
                    phase = 0;
                }
                break;
        }
    }

    public void render(SpriteBatch sb)
    {
        //render background
        sb.setColor(c);
        drawTexture(sb, background, 0, 0, BG_SIZE);

        if (phase == -1)
        {
            //display controls
        }
    }

    //rendering utility

    //These methods draw/scale/rotate whatever they are passed based on the scale/position/angle of the minigame.
    public void drawTexture(SpriteBatch sb, Texture t, float cX, float cY, int size)
    {
        drawTexture(sb, t, cX, cY, 0, size, size, false, false);
    }
    public void drawTexture(SpriteBatch sb, Texture t, float cX, float cY, float angle, int baseWidth, int baseHeight, boolean flipX, boolean flipY)
    {
        sb.draw(t, x + cX - baseWidth / 2.0f, y + cY - baseHeight / 2.0f, -(cX - baseWidth / 2.0f), -(cY - baseHeight / 2.0f), baseWidth, baseHeight, scale, scale, this.angle + angle, 0, 0, baseWidth, baseHeight, flipX, flipY);
    }


    // Position/Scale control
    private float initialScale = 0.0f; //scale at start of transform
    private float targetScale = 0.0f; //scale to end at
    private float scaleProgress = 0.0f; //time elapsed
    private float scaleTime = 2.0f; //time to perform transform

    public void setScale(float newScale)
    {
        this.scale = newScale;
    }

    public void transformScale(float targetScale, float time)
    {
        initialScale = scale;
        this.targetScale = targetScale;
        scaleProgress = 0.0f;
        scaleTime = time;
    }

    public float getMaxScale()
    {
        float maxSize = 0.8f * Math.min(Settings.WIDTH, Settings.HEIGHT); //Settings.HEIGHT will pretty much always be smaller but maybe someone fucked with something, who knows

        float ratio = ((int)(maxSize / SIZE * 2)) / 2.0f; //round to lower 0.5f

        if (ratio < 0.5f)
        {
            ratio = maxSize / SIZE;
        }

        return ratio;
    }

    public void getReward() {
        //figure out how this works later
    }

    public enum MINIGAME_RESULT {
        SUCCESS, //maybe some in-between results idk
        FAILURE
    }

    //Input binding stuff

    protected abstract BindingGroup getBindings();

    //Other general utility stuff
    public boolean isWithinArea(float x, float y)
    {
        float offset = SIZE * scale * 0.5f;
        return x >= this.x - offset && x <= this.x + offset &&
                y >= this.y - offset && y <= this.y + offset;
    }

    //converts a Vector2 based on the bottom left of screen to a vector based on current x and y of game
    public Vector2 getRelativeVector(Vector2 base)
    {
        Vector2 cpy = base.cpy();
        cpy.x -= x;
        cpy.y -= y;

        return cpy;
    }


    //This class implements TextReceiver to not screw over the console when it disables input, by being compatible with basemod's text input stuff.
    @Override
    public boolean acceptCharacter(char c) {
        return false;
    }

    @Override
    public String getCurrentText() {
        return "";
    }

    @Override
    public void setText(String s) {
    }

    //This is for a future update of TextReceiver.
    public boolean isDone() {
        if (CardCrawlGame.isPopupOpen || AbstractDungeon.screen != AbstractDungeon.CurrentScreen.NONE || isDone || !CardCrawlGame.isInARun()) {
            blockingInput = false;
            TextInput.stopTextReceiver(this);
            if (Input.processor.inactiveBindings == null) {
                if (isDone)
                {
                    Input.clearBindings();
                }
                else
                {
                    Input.processor.deactivate();
                }
            }
            return true;
        }
        return false;
    }
}