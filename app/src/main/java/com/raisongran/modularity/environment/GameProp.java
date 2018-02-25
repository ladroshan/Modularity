package com.raisongran.modularity.environment;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.os.Handler;

import com.raisongran.modularity.activity.MainActivity;
import com.raisongran.modularity.processing.LocalMath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.TimeoutException;


public class GameProp {
    private String name;
    public float x, y, z; // current cords
    private GameBehaviour[] behaviours;
    private GameSoundSource sound = null;
    private Handler handler = new Handler();

    private double _x, _y, _z; // last iteration cords
    private float _angle = 0, _speed = 3, _r = 20;

    GameProp(JSONObject mPropJson) {
        try {
            name = mPropJson.getString("name");
            x = mPropJson.getInt("x");
            y = mPropJson.getInt("y");
            z = mPropJson.getInt("z");
            JSONArray behaviourData = mPropJson.getJSONArray("behaviours");
            behaviours = new GameBehaviour[behaviourData.length()];
            for (int i = 0; i < behaviourData.length(); i++) {
                behaviours[i] = new GameBehaviour(behaviourData.getJSONObject(i));
            }
            if (mPropJson.has("sound")) {
                sound = new GameSoundSource(mPropJson.getJSONObject("sound"));
            } else {
                sound = null;
            }
        } catch(JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void setPosition(float nx, float ny, float nz) {
        x = nx; y = ny; z = nz;
        moveSound();
    }

    private void playSound() {
        if(sound != null) {
            sound.playAt(x, y, z);
        }
    }

    private void moveSound() {
        if(sound != null) {
             sound.moveTo(x, y, z);
        }
    }

    void start() {
        for (int i=0; i<behaviours.length; i++) {
            if (Objects.equals(behaviours[i].trigger, "onstart")) {
                switch (behaviours[i].action) {
                    case "play_sound":
                        playSound();
                        break;
                    case "move_around":
                        startMoveAround(behaviours[i].parameters);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    void tick(float dt) {
        for (int i=0; i<behaviours.length; i++) {
            if (MainActivity.gameManager.getGameWorld() != null) {
                if (Objects.equals(behaviours[i].trigger, "ontick")) {
                    switch (behaviours[i].action) {
                        case "follow_compass":
                            followCompass(behaviours[i].parameters);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void followCompass(String parametersJson) {
        float r = 20;
        double angleBias = 0f;
        try {
            JSONObject param = new JSONObject(parametersJson);
            r = (float) param.getInt("distance");
            angleBias = (float) param.getInt("angleBias");
        } catch (JSONException e) {
            e.printStackTrace();
            MainActivity.gameManager.stop();
        }
        float biasX = MainActivity.gameManager.getGameWorld().player.x;
        float biasY = MainActivity.gameManager.getGameWorld().player.y;
        float biasZ = MainActivity.gameManager.getGameWorld().player.z;
        double alpha = 0.07f;
        double angle = MainActivity.gameManager.angle + angleBias;
        _x = LocalMath.SmoothFilter.Get(Math.sin(angle), alpha, _x);
        _y = LocalMath.SmoothFilter.Get(Math.cos(angle), alpha, _y);
        _z = LocalMath.SmoothFilter.Get(Math.cos(angle)/6, alpha, _z);
        float x = (float) (r * _x + biasX);
        float y = (float) (r * _y + biasY);
        float z = (float) (r * _z + biasZ);
        this.setPosition(x, y, z);
    }

    private void startMoveAround(String parametersJson) {
        try {
            JSONObject param = new JSONObject(parametersJson);
            _speed = (float) param.getDouble("speed");
            _angle = (float) param.getDouble("angleBias");
            _r = (float) param.getDouble("distance");
        } catch (JSONException e) {
            e.printStackTrace();
            MainActivity.gameManager.stop();
        }
        handler.postDelayed(runnable, 50);
    }

    private void moveAround() {
        float biasX = MainActivity.gameManager.getGameWorld().player.x;
        float biasY = MainActivity.gameManager.getGameWorld().player.y;
        float biasZ = MainActivity.gameManager.getGameWorld().player.z;
        double angle = 2 * Math.PI * _angle / 360;
        float x = (float) (_r * Math.sin(angle) + biasX);
        float y = (float) (_r * Math.cos(angle) + biasY);
        this.setPosition(x, y, biasZ);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            _angle += _speed;
            if (_angle >= 360 || _angle <= -360)
                _angle = 0;
            moveAround();
            handler.postDelayed(runnable, 50);
        }
    };

    void stopSound() {
        if(sound != null) {
            sound.stop();
        }
    }

    void drawTo(Canvas canvas) {
        canvas.translate(x, y);
        // draw self
        Paint paint = new Paint();
        paint.setStrokeWidth(3f);
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(0, 0, 4, paint);
        // draw associated
        canvas.translate(-x, -y);
    }
}
