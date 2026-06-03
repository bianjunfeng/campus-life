import { showAlert } from './dialog'

export const notify = (message: string, duration = 1800): void => {
  void showAlert(String(message ?? ''), duration)
}
