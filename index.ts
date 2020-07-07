import { NativeModules, Platform } from 'react-native';

const { GmUtils } = NativeModules;

const isIOS = Platform.OS === 'ios';

export const changeNavigationBarColor = (color: string, light: boolean, animated: boolean) => {
    if (isIOS) {
        return;
    }
    GmUtils.changeNavigationBarColor(color, light, animated);
}

export * from './src'