import { Pressable, StyleSheet, Text } from 'react-native'

type Props = {
  label: string
  onPress: () => void
  disabled?: boolean
  variant?: 'primary' | 'secondary'
}

export function ActionButton({
  label,
  onPress,
  disabled = false,
  variant = 'primary',
}: Props) {
  const isSecondary = variant === 'secondary'

  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        isSecondary && styles.secondary,
        disabled && styles.disabled,
        pressed && styles.pressed,
      ]}
    >
      <Text style={[styles.label, isSecondary && styles.secondaryLabel]}>
        {label}
      </Text>
    </Pressable>
  )
}

const styles = StyleSheet.create({
  button: {
    backgroundColor: '#111827',
    borderRadius: 10,
    flex: 1,
    paddingVertical: 12,
  },
  secondary: {
    backgroundColor: '#e5e7eb',
    flex: 0,
    paddingHorizontal: 16,
  },
  disabled: {
    opacity: 0.5,
  },
  pressed: {
    opacity: 0.7,
  },
  label: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
    textAlign: 'center',
  },
  secondaryLabel: {
    color: '#111827',
  },
})
