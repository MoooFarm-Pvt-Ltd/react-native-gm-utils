import { NativeModules, Platform } from 'react-native';

const { GmUtils } = NativeModules;

const isIOS = Platform.OS === 'ios';

export const changeNavigationBarColor = (color: string, light: boolean, animated: boolean) => {
    if (isIOS) {
        return;
    }
    GmUtils.changeNavigationBarColor(color, light, animated);
}

export const unlock = () => {
    if (isIOS) {
        return;
    }
    GmUtils.unlock();
}

export const backToForeground = (color: string, light: boolean, animated: boolean) => {
    if (isIOS) {
        return;
    }
    GmUtils.backToForeground();
}

export const askForOverlayPermissions = () => {
    if (isIOS) {
        return;
    }
    GmUtils.askForOverlayPermissions();
}

export * from './src'