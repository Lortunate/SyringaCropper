use std::borrow::Cow;

use crate::core;

pub enum Operation {
    RectCrop {
        x: u32,
        y: u32,
        w: u32,
        h: u32,
    },
    CircleCrop {
        x: u32,
        y: u32,
        w: u32,
        h: u32,
    },
    RoundedRectCrop {
        x: u32,
        y: u32,
        w: u32,
        h: u32,
        r: f32,
    },
    Warp {
        corners: [f32; 8],
        tw: u32,
        th: u32,
    },
    Resize {
        tw: u32,
        th: u32,
    },
}

pub struct ProcessorPipeline<'a> {
    data: Cow<'a, [u8]>,
    width: u32,
    height: u32,
}

impl<'a> ProcessorPipeline<'a> {
    pub fn new(data: &'a [u8], width: u32, height: u32) -> Self {
        Self {
            data: Cow::Borrowed(data),
            width,
            height,
        }
    }

    pub fn apply(mut self, op: Operation) -> Option<Self> {
        let (new_data, new_w, new_h) = match op {
            Operation::RectCrop { x, y, w, h } => (
                core::perform_rect(self.data.as_ref(), self.width, self.height, x, y, w, h)?,
                w,
                h,
            ),
            Operation::CircleCrop { x, y, w, h } => (
                core::perform_circle(self.data.as_ref(), self.width, self.height, x, y, w, h)?,
                w,
                h,
            ),
            Operation::RoundedRectCrop { x, y, w, h, r } => (
                core::perform_rounded_rect(
                    self.data.as_ref(),
                    self.width,
                    self.height,
                    x,
                    y,
                    w,
                    h,
                    r,
                )?,
                w,
                h,
            ),
            Operation::Warp { corners, tw, th } => (
                core::perform_warp(
                    self.data.as_ref(),
                    self.width,
                    self.height,
                    &corners,
                    tw,
                    th,
                )?,
                tw,
                th,
            ),
            Operation::Resize { tw, th } => (
                core::perform_resize(self.data.as_ref(), self.width, self.height, tw, th)?,
                tw,
                th,
            ),
        };
        self.data = Cow::Owned(new_data);
        self.width = new_w;
        self.height = new_h;
        Some(self)
    }

    pub fn into_inner(self) -> (Vec<u8>, u32, u32) {
        (self.data.into_owned(), self.width, self.height)
    }
}
