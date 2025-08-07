package customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.aicomsapp.R

class EditTextPassword : AppCompatEditText {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun init() {
        // Set default password hidden state
        transformationMethod = PasswordTransformationMethod.getInstance()

        // Set gravity to keep the text centered vertically
        gravity = android.view.Gravity.CENTER
        filters = arrayOf(android.text.InputFilter.LengthFilter(30))

        // Add validation for password length
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Show a warning if password is too short
                if (s.toString().length < 8) {
                    setError(context.getString(R.string.passwordwarning), null)
                } else {
                    error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
