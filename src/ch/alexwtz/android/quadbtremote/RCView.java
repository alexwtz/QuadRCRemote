package ch.alexwtz.android.quadbtremote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RCView extends View {

	private Paint drawingPaint = null;

	private Point small_circle_1 = new Point();
	private Point small_circle_2 = new Point();
	private int small_circle1_r = 10;
	private int small_circle2_r = 10;

	private int left_sqare_x = 0;
	private int left_square_y = 0;
	private int left_square_side = 250;

	private int right_sqare_x = 0;
	private int right_square_y = 0;
	private int right_square_side = 250;

	private int margin = 50;

	private int screenWidth, screenHeight;

	private boolean insideCircle1 = false;
	private boolean insideCircle2 = false;


	public RCView(Context context) {
		super(context);

		initialize(context);
	}

	public RCView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		initialize(context);
	}

	public RCView(Context context, AttributeSet attrs) {
		super(context, attrs);

		initialize(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawRect(left_sqare_x, left_square_y, left_sqare_x
				+ left_square_side, left_square_y + left_square_side,
				drawingPaint);
		canvas.drawRect(right_sqare_x, right_square_y, right_sqare_x
				+ right_square_side, right_square_y + right_square_side,
				drawingPaint);

		if (insideCircle1) {
			canvas.drawCircle(small_circle_1.x, small_circle_1.y,
					small_circle1_r, drawingPaint);

		}
		if (insideCircle2) {
			canvas.drawCircle(small_circle_2.x, small_circle_2.y,
					small_circle2_r, drawingPaint);

		}
		invalidate();

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);

		int action = event.getAction() & MotionEvent.ACTION_MASK;

		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			if (isInsideLeftSquare((int) event.getX(), (int) event.getY())) {
				small_circle_1.x = (int) event.getX();
				small_circle_1.y = (int) event.getY();
				insideCircle1 = true;
			} else if (isInsideRightSquare((int) event.getX(),
					(int) event.getY())) {
				small_circle_2.x = (int) event.getX();
				small_circle_2.y = (int) event.getY();
				insideCircle2 = true;
			}
			invalidate();

			break;
		}
		case MotionEvent.ACTION_POINTER_DOWN: {
			if (isInsideLeftSquare((int) event.getX(1), (int) event.getY(1))) {
				small_circle_1.x = (int) event.getX(1);
				small_circle_1.y = (int) event.getY(1);
				insideCircle1 = true;
			} else if (isInsideRightSquare((int) event.getX(1),
					(int) event.getY(1))) {
				small_circle_2.x = (int) event.getX(1);
				small_circle_2.y = (int) event.getY(1);
				insideCircle2 = true;
			}
			invalidate();

			break;
		}
		case MotionEvent.ACTION_POINTER_UP: {
			

			break;
		}
		case MotionEvent.ACTION_MOVE: {
			for (int i = 0; i < event.getPointerCount(); i++) {
				if (isInsideLeftSquare((int) event.getX(i), (int) event.getY(i))) {
					small_circle_1.x = (int) event.getX(i);
					small_circle_1.y = (int) event.getY(i);
					insideCircle1 = true;
				} else if (isInsideRightSquare((int) event.getX(i),
						(int) event.getY(i))) {
					small_circle_2.x = (int) event.getX(i);
					small_circle_2.y = (int) event.getY(i);
					insideCircle2 = true;
				}
			}
			invalidate();
			break;
		}
		}

		return true;
	}

	private void initialize(Context context) {
		drawingPaint = new Paint();
		drawingPaint.setColor(Color.RED);
		drawingPaint.setStyle(Paint.Style.STROKE);
		drawingPaint.setAntiAlias(true);

	}

	private boolean isInsideLeftSquare(int x, int y) {
		if (x >= left_sqare_x + small_circle1_r
				&& x <= left_sqare_x + left_square_side - small_circle1_r
				&& y >= left_square_y + small_circle1_r
				&& y <= left_square_y + left_square_side - small_circle1_r)
			return true;
		else
			return false;
	}

	private boolean isInsideRightSquare(int x, int y) {
		if (x >= right_sqare_x + small_circle1_r
				&& x <= right_sqare_x + right_square_side - small_circle1_r
				&& y >= right_square_y + small_circle1_r
				&& y <= right_square_y + right_square_side - small_circle1_r)
			return true;
		else
			return false;
	}

	public void setScreenSize(Point size) {
		this.screenWidth = size.x;
		this.screenHeight = size.y;
		left_sqare_x = margin;
		right_sqare_x = screenWidth - margin - right_square_side;
		left_square_y = margin;
		right_square_y = margin;
	}
	
	public Point getLeftValues(){
		return small_circle_1;
	}
	
	public Point getRightValues(){
		return small_circle_2;
	}
}