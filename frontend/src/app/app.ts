import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastContainer } from './components/toast-container/toast-container';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastContainer],
  template: '<router-outlet /><app-toast-container />',
  styles: ':host { display: block; min-height: 100vh; }',
})
export class App {}
