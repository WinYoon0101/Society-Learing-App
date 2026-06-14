package com.example.frontend.ui.feed;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DotsIndicatorDecoration extends RecyclerView.ItemDecoration {

    // Bạn có thể đổi mã màu ở đây cho hợp với màu chủ đạo của App
    private final int colorActive = Color.parseColor("#10B981"); // Màu xanh lá đang chọn
    private final int colorInactive = Color.parseColor("#E5E7EB"); // Màu xám cho các ảnh khác

    private static final float DP = Resources.getSystem().getDisplayMetrics().density;

    // Tùy chỉnh kích thước
    private final int mIndicatorHeight = (int) (DP * 24); // Chiều cao khu vực chứa dấu chấm
    private final float mIndicatorItemLength = DP * 6; // Kích thước dấu chấm (Đường kính)
    private final float mIndicatorItemPadding = DP * 8; // Khoảng cách giữa các chấm

    private final Paint mPaint = new Paint();

    public DotsIndicatorDecoration() {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        int itemCount = parent.getAdapter() != null ? parent.getAdapter().getItemCount() : 0;

        // Nếu bài viết chỉ có 1 ảnh thì ẩn luôn dấu chấm
        if (itemCount <= 1) {
            return;
        }

        // Logic tìm ảnh nào đang được cuộn vào giữa màn hình nhất
        int activePosition = 0;
        float center = parent.getWidth() / 2f;
        float minDistance = Float.MAX_VALUE;

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            float childCenter = child.getLeft() + child.getWidth() / 2f;
            float distance = Math.abs(center - childCenter);
            if (distance < minDistance) {
                minDistance = distance;
                activePosition = parent.getChildAdapterPosition(child);
            }
        }

        if (activePosition == RecyclerView.NO_POSITION) {
            return;
        }

        // Tính toán để căn giữa nguyên cụm dấu chấm
        float totalLength = mIndicatorItemLength * itemCount;
        float paddingBetweenItems = Math.max(0, itemCount - 1) * mIndicatorItemPadding;
        float indicatorTotalWidth = totalLength + paddingBetweenItems;
        float indicatorStartX = (parent.getWidth() - indicatorTotalWidth) / 2f;

        // Xác định vị trí vẽ dấu chấm (Mép dưới cùng)
        float indicatorPosY = parent.getHeight() - (mIndicatorHeight / 2f);

        // Tiến hành vẽ
        drawInactiveIndicators(c, indicatorStartX, indicatorPosY, itemCount);
        drawActiveIndicator(c, indicatorStartX, indicatorPosY, activePosition);
    }

    private void drawInactiveIndicators(Canvas c, float indicatorStartX, float indicatorPosY, int itemCount) {
        mPaint.setColor(colorInactive);
        float itemWidth = mIndicatorItemLength + mIndicatorItemPadding;
        float start = indicatorStartX;
        for (int i = 0; i < itemCount; i++) {
            c.drawCircle(start + mIndicatorItemLength / 2f, indicatorPosY, mIndicatorItemLength / 2f, mPaint);
            start += itemWidth;
        }
    }

    private void drawActiveIndicator(Canvas c, float indicatorStartX, float indicatorPosY, int highlightPosition) {
        mPaint.setColor(colorActive);
        float itemWidth = mIndicatorItemLength + mIndicatorItemPadding;
        float highlightStart = indicatorStartX + itemWidth * highlightPosition;
        c.drawCircle(highlightStart + mIndicatorItemLength / 2f, indicatorPosY, mIndicatorItemLength / 2f, mPaint);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        // Tự động chừa 1 khoảng trống (padding bottom) 24dp ở mép dưới ảnh để có chỗ vẽ dấu chấm
        outRect.bottom = mIndicatorHeight;
    }
}