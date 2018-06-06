import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { HttpModule } from '@angular/http';
import { HttpClientModule }    from '@angular/common/http';

import { TweetService } from './tweet/tweet.service';


import { AppComponent } from './app.component';
import { TweetComponent } from './tweet/tweet.component';


@NgModule({
  declarations: [
    AppComponent,
    TweetComponent
  ],
  imports: [
    BrowserModule,
    HttpModule,
    HttpClientModule,
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
